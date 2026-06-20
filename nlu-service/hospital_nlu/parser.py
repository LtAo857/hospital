from __future__ import annotations

import json
import re
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

from . import constants, rules
from .neo4j import Neo4jManager


class IntentSlotParser:
    # Backward-compatible class-level constants (delegated to constants module)
    CONSULT_HINT_KEYWORDS = constants.CONSULT_HINT_KEYWORDS
    URGENT_KEYWORDS = constants.URGENT_KEYWORDS
    HEART_KEYWORDS = constants.HEART_KEYWORDS
    BREATH_KEYWORDS = constants.BREATH_KEYWORDS
    BREAST_KEYWORDS = constants.BREAST_KEYWORDS
    DANGEROUS_KEYWORDS = constants.DANGEROUS_KEYWORDS
    SYMPTOM_SYNONYMS = constants.SYMPTOM_SYNONYMS

    def __init__(self, model_path: Path = constants.DEFAULT_MODEL_PATH):
        self.model = self._load_model(model_path)
        self.departments: List[str] = rules.normalize_departments(
            None, self.model.get("departments", [])
        )
        self.symptom_department_map: Dict[str, str] = self.model.get("symptomDepartmentMap", {})
        self.llm_enabled = True
        self.llm_api_key = "sk-03747d36451b4a9ca5a01fb5608575fd"
        self.llm_base_url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        self.llm_model = "qwen-plus"
        self.llm_timeout = 5
        self.llm_min_confidence = 0.7
        self.neo4j = Neo4jManager()

    # ── main entry ──────────────────────────────────────────

    def parse(self, text: str, departments: Optional[List[str]] = None) -> Dict[str, Any]:
        started_at = time.perf_counter()
        source = "rules"
        engine = self.model.get("engine", "local-rule")
        engine_model = self.model.get("name", "hospital-nlu-v1")

        dept_list = rules.normalize_departments(departments, self.model.get("departments", []))

        llm_result = None
        if self.llm_enabled and self.llm_api_key:
            llm_result = self._llm_parse(text, dept_list)

        if llm_result is not None and llm_result.get("confidence", 0) >= self.llm_min_confidence:
            intent = llm_result.get("intent", "unknown")
            slots = llm_result.get("slots", {})
            confidence = llm_result.get("confidence", 0.5)
            if intent in ("unknown", "unsupported") and rules._contains_any(
                rules._normalize(text), constants.REGISTRATION_KEYWORDS
            ):
                intent = "registration"
            # Normalize symptom/symptoms
            if "symptoms" not in slots or not isinstance(slots.get("symptoms"), list):
                if "symptom" in slots and isinstance(slots.get("symptom"), str) and slots["symptom"]:
                    slots["symptoms"] = [slots["symptom"]]
                else:
                    slots["symptoms"] = []
            if "symptom" not in slots or not slots.get("symptom"):
                if slots.get("symptoms"):
                    slots["symptom"] = slots["symptoms"][0]
            source = "llm"
            engine = "llm-remote"
            engine_model = self.llm_model
        else:
            normalized = rules._normalize(text)
            intent = rules.detect_intent(normalized, dept_list, self.symptom_department_map)
            all_symptoms = rules.extract_all_symptoms(normalized, self.symptom_department_map)
            slots = {
                "symptoms": all_symptoms,
                "symptom": all_symptoms[0] if all_symptoms else None,
                "department": rules.extract_department(normalized, dept_list),
                "doctorName": rules.extract_doctor_name(normalized),
                "date": rules.extract_date(normalized),
                "timePreference": rules.extract_time_preference(normalized),
                "patientGender": rules.extract_patient_gender(normalized),
                "doctorGender": rules.extract_doctor_gender_preference(normalized),
                "doctorAgePreference": rules.extract_doctor_age_preference(normalized),
                "population": rules.extract_population(normalized),
            }
            # Useful slots or a strong intent both count as "we understood enough" —
            # context follow-ups like "今天呢" or "帮她挂" rely on session memory.
            has_slot = bool(
                slots.get("date") or slots.get("department")
                or slots.get("doctorName") or all_symptoms
            )
            has_strong_intent = intent in (
                "registration", "query_doctor", "query_department", "explain_recommendation",
                "query_message", "query_user_card",
            )
            confidence = 0.9 if (has_slot or has_strong_intent) else 0.0

        if not isinstance(slots, dict):
            slots = {}

        normalized_text = rules._normalize(text)
        if not slots.get("patientGender"):
            slots["patientGender"] = rules.extract_patient_gender(normalized_text)
        if not slots.get("doctorGender"):
            slots["doctorGender"] = rules.extract_doctor_gender_preference(normalized_text)
        if not slots.get("doctorAgePreference"):
            slots["doctorAgePreference"] = rules.extract_doctor_age_preference(normalized_text)
        if not slots.get("population"):
            slots["population"] = rules.extract_population(normalized_text)

        # Ensure symptoms array is consistent
        if not isinstance(slots.get("symptoms"), list) or not slots.get("symptoms"):
            if isinstance(slots.get("symptom"), str) and slots["symptom"]:
                slots["symptoms"] = [slots["symptom"]]
            else:
                slots["symptoms"] = []
        if not slots.get("symptom") and slots.get("symptoms"):
            slots["symptom"] = slots["symptoms"][0]

        # Department fallback: graph → symptom map → text extraction
        if not slots.get("department"):
            for sym in slots.get("symptoms", []):
                graph_departments = self.neo4j.query_graph(sym, slots.get("population"))
                if graph_departments:
                    top_dept = graph_departments[0]["department"]
                    if top_dept in dept_list:
                        slots["department"] = top_dept
                        break
        if not slots.get("department"):
            for sym in slots.get("symptoms", []):
                mapped = self.symptom_department_map.get(sym)
                if not mapped:
                    for key in self.symptom_department_map:
                        if key in sym or sym in key:
                            mapped = self.symptom_department_map[key]
                            break
                if mapped and mapped in dept_list:
                    slots["department"] = mapped
                    break
        if not slots.get("department"):
            dept_from_text = rules.extract_department(normalized_text, dept_list)
            if dept_from_text:
                slots["department"] = dept_from_text

        # Build symptom-dept graph and disease info for cross-validation (both LLM & rules paths)
        symptom_dept_graph: Dict[str, List[str]] = {}
        symptom_disease_info: Dict[str, List[Dict[str, str]]] = {}
        for sym in slots.get("symptoms", []):
            if not sym:
                continue
            graph_depts = self.neo4j.query_graph(sym, slots.get("population"))
            if graph_depts:
                symptom_dept_graph[sym] = [d["department"] for d in graph_depts]
            elif sym in self.symptom_department_map:
                symptom_dept_graph[sym] = [self.symptom_department_map[sym]]
            diseases = self.neo4j.query_disease_by_symptom(sym)
            if diseases:
                symptom_disease_info[sym] = diseases

        # Medical QA: extract disease entity, query Neo4j, and format answer in Python
        # so Java doesn't need a second LLM call.
        disease_info = None
        medical_qa_answer = None
        if intent == "medical_qa":
            disease_name = slots.get("diseaseName") or rules.extract_disease_name(normalized_text)
            if disease_name:
                slots["diseaseName"] = disease_name
                disease_info = self.neo4j.query_disease_full(disease_name)
                if disease_info:
                    medical_qa_answer = self._format_disease_answer(disease_info)

        # Consult depth flag — registration and consultation can co-exist.
        # e.g. "胸疼，明天挂号，开什么药" → both book & ask about medication.
        has_consult = bool(
            slots.get("symptoms")
            and rules._contains_any(normalized_text, constants.CONSULT_DEMAND_KEYWORDS)
        )
        slots["hasConsultDemand"] = has_consult

        latency_ms = max(1, int((time.perf_counter() - started_at) * 1000))

        return {
            "intent": intent,
            "slots": slots,
            "confidence": confidence,
            "source": source,
            "engine": engine,
            "model": engine_model,
            "latencyMs": latency_ms,
            "symptomDeptGraph": symptom_dept_graph,
            "symptomDiseaseInfo": symptom_disease_info,
            "diseaseInfo": disease_info,
            "medicalQaAnswer": medical_qa_answer,
            "accelerations": ["structured-output", "business-fallback-ready", "graph-disease-facts", "medical-qa"],
        }

    # ── LLM ──────────────────────────────────────────────────

    def _llm_parse(self, text: str, departments: List[str]) -> Optional[Dict[str, Any]]:
        import urllib.request

        graph_context = self._query_graph_for_text(text)

        body = json.dumps({
            "model": self.llm_model,
            "temperature": 0.1,
            "messages": [
                {"role": "system", "content": self._build_system_prompt(departments, graph_context)},
                {"role": "user", "content": text},
            ],
            "response_format": {"type": "json_object"},
        }).encode("utf-8")

        req = urllib.request.Request(
            self.llm_base_url,
            data=body,
            headers={
                "Authorization": f"Bearer {self.llm_api_key}",
                "Content-Type": "application/json",
            },
        )
        try:
            resp = urllib.request.urlopen(req, timeout=self.llm_timeout)
            raw = json.loads(resp.read().decode("utf-8"))
            content = raw.get("choices", [{}])[0].get("message", {}).get("content", "")
            if not content:
                return None
            result = json.loads(self._extract_json(content))
            if "intent" not in result:
                return None
            if "slots" not in result or not isinstance(result.get("slots"), dict):
                result["slots"] = {}
            if "confidence" not in result:
                result["confidence"] = 0.5

            dept = result.get("slots", {}).get("department")
            symptom = result.get("slots", {}).get("symptom") or ""
            allowed_departments = set(departments)
            if not dept or dept not in allowed_departments:
                mapped = self.symptom_department_map.get(symptom)
                if not mapped and symptom:
                    for key in self.symptom_department_map:
                        if key in symptom or symptom in key:
                            mapped = self.symptom_department_map[key]
                            result["slots"]["symptom"] = key
                            break
                if mapped and mapped in allowed_departments:
                    result["slots"]["department"] = mapped
                else:
                    result["slots"]["department"] = None

            if not result["slots"].get("patientGender"):
                result["slots"]["patientGender"] = rules.extract_patient_gender(text)
            if not result["slots"].get("doctorGender"):
                result["slots"]["doctorGender"] = rules.extract_doctor_gender_preference(text)
            if not result["slots"].get("doctorAgePreference"):
                result["slots"]["doctorAgePreference"] = rules.extract_doctor_age_preference(text)
            return result
        except Exception:
            return None

    def _build_system_prompt(self, departments: List[str], graph_context: Optional[str] = None) -> str:
        dept_list = "、".join(departments)
        prompt = (
            "你是医院挂号NLU引擎。从用户输入中提取意图和槽位，输出严格JSON。\n"
            "\n"
            f"当前可用科室：{dept_list}\n"
        )
        if graph_context:
            prompt += (
                "\n"
                f"[知识图谱查询结果]\n{graph_context}\n"
                "以上图谱结果是基于真实症状-科室关联数据查询的，优先参考图谱推荐的科室。\n"
            )
        prompt += (
            "\n"
            "意图类型：\n"
            "- registration: 发起挂号/预约行为（如'我要挂内科''帮我预约明天的号'）。注意：'挂什么科/挂哪个科/看什么科/有哪些科室'是询问科室建议，不是挂号，应归为query_department\n"
            "- medical_qa: 询问吃什么药、怎么治、如何预防、注意事项、做什么检查等医疗知识问答（不含挂号意图）\n"
            "- query_department: 询问科室建议（如'挂什么科''看什么科''有哪些科室''我该挂哪个科'）\n"
            "- query_doctor: 查医生信息、查号源、医生排班、某科室有哪些医生（如'张医生明天有号吗''心内科有哪些医生'）\n"
            "- query_message: 查消息、通知、提醒\n"
            "- query_user_card: 查就诊卡、建卡、实名信息\n"
            "- explain_recommendation: 询问为什么推荐、解释理由\n"
            "- unsupported: 取消挂号、退号等暂不支持的操作\n"
            "- dangerous: 高危操作，如删库、删表、批量修改、执行系统命令、提权等\n"
            "- unknown: 无法判断\n"
            "\n"
            "槽位字段(slots)：\n"
            "- symptoms: 症状列表（数组），如[\"胸疼\",\"头疼\"]；即使用户只说一个症状也用数组格式\n"
            "- symptom: 第一个症状（与symptoms[0]一致，用于向后兼容）\n"
            "- department: 科室名，必须从可用科室列表中选择，可从症状推断。图谱有推荐结果时优先采用图谱推荐\n"
            "- doctorName: 医生姓名\n"
            "- date: 日期，保持原文，例如 今天/明天/2026-06-01\n"
            "- timePreference: 时段偏好，如上午、下午、最早\n"
            "- patientGender: 患者自述性别，女/男/null，和医生偏好分开\n"
            "- doctorGender: 用户明确希望的医生性别，女/男/null；只有明确偏好才填\n"
            "- doctorAgePreference: 用户对医生年龄的偏好，年长/年轻/null（未提及填null）\n"
            "- population: 患者人群，儿童/成人/孕妇/老人/null\n"
            "- diseaseName: 用户询问的疾病名称（仅medical_qa意图时提取）\n"
            "\n"
            "规则：\n"
            "1. department必须是可用科室列表中的科室，不要编造不存在的科室\n"
            "2. 日期保持用户原文，不要转换成其他格式\n"
            "3. 能从症状推断科室时填入department，不确定填null\n"
            "4. 如果用户说的是自己的性别/年龄/身份信息，填到patientGender或相关患者上下文里，不要和doctorGender混淆\n"
            "5. doctorGender只在用户明确说想找男医生/女医生时填入；患者自述性别不要直接当成doctorGender\n"
            "6. 没有把握的字段填null，不要编造\n"
            "7. confidence取值0.0~1.0，基于你对该结果的确定程度\n"
            "8. 只输出JSON，不要任何额外文字\n"
            "9. 所有症状相关咨询统一归类为registration，系统内部通过hasConsultDemand区分挂号与纯咨询。但用户问'挂什么科/看什么科/有哪些科室'时是询问科室建议，不是挂号，应归为query_department。\n"
            "10. 用户提到挂/挂号/预约/号/看诊等挂号行为词（非询问'挂什么科'）时，hasConsultDemand应为false。\n"
        )
        return prompt

    # ── helpers ─────────────────────────────────────────────

    def _load_model(self, model_path: Path) -> Dict[str, Any]:
        if not model_path.exists():
            return dict(constants.DEFAULT_MODEL)
        with model_path.open("r", encoding="utf-8") as f:
            loaded = json.load(f)
        merged = dict(constants.DEFAULT_MODEL)
        merged.update(loaded)
        return merged

    def _query_graph_for_text(self, text: str) -> Optional[str]:
        symptom = rules.extract_symptom(rules._normalize(text), self.symptom_department_map)
        if not symptom:
            return None
        pop = rules.extract_population(rules._normalize(text))
        departments = self.neo4j.query_graph(symptom, pop)
        if not departments:
            return None
        lines = []
        pop_hint = f"(人群:{pop})" if pop else ""
        for d in departments:
            lines.append(f"  症状「{symptom}」{pop_hint} → {d['department']}(得分:{d['totalScore']:.1f})")
        return "\n".join(lines)

    @staticmethod
    def _format_disease_answer(d: dict) -> str:
        """Format Neo4j disease data into a readable answer — no LLM needed."""
        name = d.get("name", "")
        lines = [f"关于「{name}」："]
        if d.get("desc"):
            lines.append(f"简介：{d['desc']}")
        if d.get("drugs"):
            lines.append(f"常用药物：{d['drugs']}")
        if d.get("cure_way"):
            lines.append(f"治疗方式：{d['cure_way']}")
        if d.get("prevent"):
            lines.append(f"预防建议：{d['prevent']}")
        if d.get("do_eat"):
            lines.append(f"推荐饮食：{d['do_eat']}")
        if d.get("not_eat"):
            lines.append(f"忌食：{d['not_eat']}")
        return "\n".join(lines)

    @staticmethod
    def _extract_json(content: str) -> str:
        text = content.strip()
        start = text.find("{")
        end = text.rfind("}")
        if start >= 0 and end > start:
            return text[start:end + 1]
        return text
