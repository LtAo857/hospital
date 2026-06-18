from __future__ import annotations

import re
from typing import Any, Dict, List, Optional

from . import constants


def _has_text(value: Any) -> bool:
    return value is not None and str(value).strip() != ""


def _normalize(text: str) -> str:
    return re.sub(r"\s+", "", text.strip())


def _contains_any(text: str, keywords: List[str]) -> bool:
    return any(keyword in text for keyword in keywords)


def normalize_departments(departments: Optional[List[str]], fallback: List[str]) -> List[str]:
    source = departments if departments else fallback
    if isinstance(source, str):
        source = [source]
    normalized = []
    seen = set()
    for item in source:
        name = "" if item is None else str(item).strip()
        if name and name not in seen:
            seen.add(name)
            normalized.append(name)
    return normalized or list(fallback)


# ── intent detection ──────────────────────────────────────

def detect_intent(
    text: str,
    departments: List[str],
    symptom_department_map: Dict[str, str],
) -> str:
    if _contains_any(text, constants.DANGEROUS_KEYWORDS):
        return "dangerous"
    if _contains_any(text, ["取消", "退号"]):
        return "unsupported"
    if _contains_any(text, ["为什么", "为啥", "推荐理由"]):
        return "explain_recommendation"
    if _contains_any(text, ["消息", "通知", "提醒"]):
        return "query_message"
    if _contains_any(text, ["就诊卡", "诊疗卡", "信息卡"]):
        return "query_user_card"
    if _contains_any(text, ["胸痛", "胸疼", "胸闷"]) and not _contains_any(text, constants.REGISTRATION_KEYWORDS):
        return "medical_consult"
    if _contains_any(text, constants.CONSULT_HINT_KEYWORDS) and (
        extract_symptom(text, symptom_department_map) or extract_department(text, departments)
    ):
        return "medical_consult"
    if _contains_any(text, constants.REGISTRATION_KEYWORDS):
        return "registration"
    if _contains_any(text, ["医生", "大夫", "主任"]):
        return "query_doctor"
    if extract_department(text, departments) or extract_symptom(text, symptom_department_map):
        return "registration"
    return "unknown"


# ── slot extraction ────────────────────────────────────────

def extract_department(text: str, departments: List[str]) -> Optional[str]:
    for dept in departments:
        if dept in text:
            return dept
    return None


def extract_symptom(text: str, symptom_department_map: Dict[str, str]) -> Optional[str]:
    all_symptoms = extract_all_symptoms(text, symptom_department_map)
    return all_symptoms[0] if all_symptoms else None


def extract_all_symptoms(
    text: str,
    symptom_department_map: Dict[str, str],
) -> List[str]:
    """Extract ALL symptoms from text (supports 多症状 like 胸疼+头疼)."""
    found = []
    seen: set = set()

    # Phase 1: exact keyword match
    for symptom in symptom_department_map:
        if symptom in text and symptom not in seen:
            found.append((text.find(symptom), symptom))
            seen.add(symptom)

    # Phase 2: jieba tokenization + synonym fuzzy match
    fuzzy_matches = _fuzzy_symptom_match_all(text, symptom_department_map)
    for matched in fuzzy_matches:
        if matched not in seen:
            found.append((text.find(matched) if matched in text else 999, matched))
            seen.add(matched)

    # Phase 3: generic pain/symptom patterns
    for m in re.finditer(r'([\u4e00-\u9fa5]{1,4})(?:疼|痛|酸|胀|肿|不舒服|难受|出血|发炎)', text):
        raw = m.group(0)
        for prefix in ['我想去看', '我想去', '我想', '我要去', '我要', '我去', '我', '想', '去', '有点', '有些', '的']:
            if raw.startswith(prefix) and len(raw) > len(prefix) + 1:
                raw = raw[len(prefix):]
                break
        if raw not in seen:
            found.append((m.start(), raw))
            seen.add(raw)

    found.sort(key=lambda x: x[0])
    return [item[1] for item in found]


def _fuzzy_symptom_match_all(
    text: str,
    symptom_department_map: Dict[str, str],
) -> List[str]:
    """Use jieba tokenization + symptom synonym dictionary to match colloquial expressions."""
    try:
        import jieba
    except ImportError:
        return []

    tokens = jieba.lcut(text)
    if not tokens:
        return []

    matches = []
    seen: set = set()
    for token in tokens:
        for canonical, variants in constants.SYMPTOM_SYNONYMS.items():
            if canonical in seen:
                continue
            if token in variants or token == canonical:
                if canonical in symptom_department_map:
                    matches.append(canonical)
                    seen.add(canonical)
                break
            for variant in variants:
                if len(token) >= 3 and len(variant) >= 3:
                    if token in variant or variant in token:
                        if canonical in symptom_department_map:
                            matches.append(canonical)
                            seen.add(canonical)
                        break
    return matches


def extract_doctor_name(text: str) -> Optional[str]:
    match = re.search(r"([\u4e00-\u9fa5]{1,4})(?:医生|大夫|主任)", text)
    if not match:
        return None
    full_match = match.group(0)
    if re.search(r"(?:女医生|男医生|女性医生|男性医生)", full_match):
        return None
    name = match.group(1)
    for noise in ("帮我", "查一下", "一下", "查", "挂", "找", "看", "想", "预约", "我只要", "我要", "我想找", "我想看", "优先"):
        name = name.replace(noise, "")
    stop_words = {"哪个", "哪位", "这个", "这位", "一个", "在线", "推荐", "所有", "查查", "女", "男"}
    if name in stop_words:
        return None
    if name.endswith(("找", "看", "挂")) and len(name) > 1:
        name = name[-1]
    return name or None


def extract_date(text: str) -> Optional[str]:
    relative_dates = [
        "今天", "明天", "后天", "大后天",
        "本周", "下周",
        "周一", "周二", "周三", "周四", "周五", "周六", "周日",
        "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日",
    ]
    for item in relative_dates:
        if item in text:
            return item
    match = re.search(r"(\d{1,2})[月/-](\d{1,2})[日号]?", text)
    if match:
        return match.group(0)
    return None


def extract_time_preference(text: str) -> Optional[str]:
    preferences = ["上午", "下午", "晚上", "早上", "中午", "最早", "最晚", "早点", "晚点"]
    for item in preferences:
        if item in text:
            return item
    return None


def extract_patient_gender(text: str) -> Optional[str]:
    if not _has_text(text):
        return None
    if any(keyword in text for keyword in ["我是女生", "我是女性", "女性患者", "女生", "女士", "女的", "女性"]):
        return "女"
    if any(keyword in text for keyword in ["我是男生", "我是男性", "男性患者", "男生", "男的", "男性"]):
        return "男"
    return None


def extract_doctor_gender_preference(text: str) -> Optional[str]:
    if not _has_text(text):
        return None
    explicit_female = ["女医生", "女性医生", "找女医生", "要女医生", "希望女医生", "想看女医生", "优先女医生"]
    explicit_male = ["男医生", "男性医生", "找男医生", "要男医生", "希望男医生", "想看男医生", "优先男医生"]
    if any(keyword in text for keyword in explicit_female):
        return "女"
    if any(keyword in text for keyword in explicit_male):
        return "男"
    return None


def extract_doctor_age_preference(text: str) -> Optional[str]:
    if not _has_text(text):
        return None
    if any(keyword in text for keyword in ["年长", "资深", "专家", "主任", "老专家"]):
        return "年长"
    if any(keyword in text for keyword in ["年轻", "青年", "年轻点", "新医生"]):
        return "年轻"
    return None


def extract_population(text: str) -> Optional[str]:
    if not _has_text(text):
        return None
    if any(kw in text for kw in ["小孩", "儿童", "宝宝", "幼儿", "婴儿", "孩子", "小儿"]):
        return "儿童"
    if any(kw in text for kw in ["老人", "老年", "长辈", "高龄", "年纪大"]):
        return "老人"
    if any(kw in text for kw in ["孕妇", "怀孕", "孕期", "产妇"]):
        return "孕妇"
    if any(kw in text for kw in ["成人", "成年人", "大人"]):
        return "成人"
    return None
