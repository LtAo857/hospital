from __future__ import annotations

import json
import re
import time
from pathlib import Path
from typing import Any, Dict, List, Optional


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MODEL_PATH = ROOT / "model" / "intent_slot_model.json"


DEFAULT_MODEL: Dict[str, Any] = {
    "name": "hospital-nlu-demo-v1",
    "engine": "local-rule-demo",
    "departments": [
        "口腔科",
        "呼吸内科",
        "消化内科",
        "心内科",
        "皮肤科",
        "眼科",
        "耳鼻喉科",
        "儿科",
        "骨科",
        "妇科",
        "神经内科",
    ],
    "symptomDepartmentMap": {
        "牙疼": "口腔科",
        "牙痛": "口腔科",
        "牙龈": "口腔科",
        "口腔": "口腔科",
        "咳嗽": "呼吸内科",
        "发烧": "呼吸内科",
        "胸闷": "心内科",
        "胃疼": "消化内科",
        "腹痛": "消化内科",
        "皮疹": "皮肤科",
        "眼睛": "眼科",
        "耳朵": "耳鼻喉科",
        "头疼": "神经内科",
        "骨折": "骨科",
    },
}


class IntentSlotParser:
    def __init__(self, model_path: Path = DEFAULT_MODEL_PATH):
        self.model = self._load_model(model_path)
        self.departments: List[str] = self.model.get("departments", [])
        self.symptom_department_map: Dict[str, str] = self.model.get(
            "symptomDepartmentMap", {}
        )

    def parse(self, text: str) -> Dict[str, Any]:
        started_at = time.perf_counter()
        normalized = self._normalize(text)

        intent = self._detect_intent(normalized)
        slots = {
            "symptom": self._extract_symptom(normalized),
            "department": self._extract_department(normalized),
            "doctorName": self._extract_doctor_name(normalized),
            "date": self._extract_date(normalized),
            "timePreference": self._extract_time_preference(normalized),
        }

        if not slots["department"] and slots["symptom"]:
            slots["department"] = self.symptom_department_map.get(slots["symptom"])

        confidence = self._confidence(intent, slots, normalized)
        latency_ms = max(1, int((time.perf_counter() - started_at) * 1000))

        return {
            "intent": intent,
            "slots": slots,
            "confidence": confidence,
            "source": "demo_rules",
            "engine": self.model.get("engine", "local-rule-demo"),
            "model": self.model.get("name", "hospital-nlu-demo-v1"),
            "latencyMs": latency_ms,
            "accelerations": ["structured-output", "business-fallback-ready"],
        }

    def _load_model(self, model_path: Path) -> Dict[str, Any]:
        if not model_path.exists():
            return DEFAULT_MODEL
        with model_path.open("r", encoding="utf-8") as f:
            loaded = json.load(f)
        merged = dict(DEFAULT_MODEL)
        merged.update(loaded)
        return merged

    def _detect_intent(self, text: str) -> str:
        if self._contains_any(text, ["取消", "退号"]):
            return "unsupported"
        if self._contains_any(text, ["为什么", "为啥", "推荐理由"]):
            return "explain_recommendation"
        if self._contains_any(text, ["消息", "通知", "提醒"]):
            return "query_message"
        if self._contains_any(text, ["就诊卡", "诊疗卡", "信息卡"]):
            return "query_user_card"
        if self._contains_any(text, ["挂", "预约", "号", "号源", "看诊", "就诊"]):
            return "registration"
        if self._contains_any(text, ["医生", "大夫", "主任"]):
            return "query_doctor"
        if self._extract_department(text) or self._extract_symptom(text):
            return "registration"
        return "unknown"

    def _extract_department(self, text: str) -> Optional[str]:
        for dept in self.departments:
            if dept in text:
                return dept
        return None

    def _extract_symptom(self, text: str) -> Optional[str]:
        candidates = []
        for symptom in self.symptom_department_map:
            index = text.find(symptom)
            if index >= 0:
                candidates.append((index, -len(symptom), symptom))
        if not candidates:
            return None
        return sorted(candidates)[0][2]

    def _extract_doctor_name(self, text: str) -> Optional[str]:
        match = re.search(r"([\u4e00-\u9fa5]{1,4})(?:医生|大夫|主任)", text)
        if not match:
            return None
        name = match.group(1)
        for noise in ("帮我", "查一下", "一下", "查", "挂", "找", "看", "想", "预约"):
            name = name.replace(noise, "")
        stop_words = {"哪个", "哪位", "这个", "这位", "一个", "在线", "推荐", "所有", "查查"}
        if name in stop_words:
            return None
        if name.endswith(("找", "看", "挂")) and len(name) > 1:
            name = name[-1]
        return name or None

    def _extract_date(self, text: str) -> Optional[str]:
        relative_dates = [
            "今天",
            "明天",
            "后天",
            "大后天",
            "本周",
            "下周",
            "周一",
            "周二",
            "周三",
            "周四",
            "周五",
            "周六",
            "周日",
            "星期一",
            "星期二",
            "星期三",
            "星期四",
            "星期五",
            "星期六",
            "星期日",
        ]
        for item in relative_dates:
            if item in text:
                return item
        match = re.search(r"(\d{1,2})[月/-](\d{1,2})[日号]?", text)
        if match:
            return match.group(0)
        return None

    def _extract_time_preference(self, text: str) -> Optional[str]:
        preferences = [
            "上午",
            "下午",
            "晚上",
            "早上",
            "中午",
            "最早",
            "最晚",
            "早点",
            "晚点",
        ]
        for item in preferences:
            if item in text:
                return item
        return None

    def _confidence(self, intent: str, slots: Dict[str, Optional[str]], text: str) -> float:
        if intent == "unknown":
            return 0.35
        score = 0.55
        if intent in {"registration", "query_doctor"}:
            score += 0.1
        if self._contains_any(text, ["挂", "预约", "查", "看", "推荐"]):
            score += 0.08
        for key in ("department", "symptom", "doctorName", "date", "timePreference"):
            if slots.get(key):
                score += 0.06
        if intent == "unsupported":
            score = max(score, 0.82)
        return round(min(score, 0.96), 2)

    def _normalize(self, text: str) -> str:
        return re.sub(r"\s+", "", text.strip())

    def _contains_any(self, text: str, keywords: List[str]) -> bool:
        return any(keyword in text for keyword in keywords)
