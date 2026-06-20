"""Extract ALL diseases from medical.json and generate Neo4j Cypher.

Matches symptoms to known Symptom nodes and departments to known Department
nodes dynamically, so any disease in the JSON gets loaded into the graph.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

MEDICAL_JSON = Path(r"D:\Code\code\Java\hospital\hospital\other\RAGQnASystem-main\data\medical.json")
OUTPUT_CYPHER = Path(__file__).resolve().parents[1] / "neo4j_disease_layer.cypher"
OUTPUT_DISEASE_NAMES = Path(__file__).resolve().parents[1] / "hospital_nlu" / "disease_names.txt"

# ── Known Neo4j nodes (must stay in sync with neo4j_init.cypher) ──────────

SYMPTOM_ALIASES: dict[str, list[str]] = {
    "牙疼":      ["牙痛", "牙酸", "牙龈肿", "牙龈出血", "口腔疼"],
    "咳嗽":      ["干咳", "咳痰", "嗓子痒", "喉咙痛", "有痰"],
    "发烧":      ["发热", "高烧", "低烧", "体温高", "发烫"],
    "胸闷":      ["心慌", "气短", "呼吸困难", "心悸", "憋气", "喘不上气"],
    "胸痛":      ["胸疼", "心口疼", "心口痛", "压榨感"],
    "胃疼":      ["胃痛", "胃酸", "胃胀", "反酸", "烧心"],
    "腹痛":      ["肚子疼", "肚子痛", "拉肚子", "腹泻", "肚子胀", "肠炎", "腹痛"],
    "皮疹":      ["过敏", "起疹", "起包", "瘙痒", "红疹", "荨麻疹", "湿疹"],
    "眼睛不适":  ["视力模糊", "看不清", "眼花", "眼睛疼", "干眼", "流泪", "眼痛", "眼红", "眼肿"],
    "耳朵不适":  ["耳鸣", "听力下降", "耳朵疼", "耳闷", "中耳炎", "耳痛"],
    "头疼":      ["头痛", "头晕", "脑壳疼", "偏头痛", "头昏", "头胀"],
    "骨折":      ["摔伤", "骨裂", "扭伤", "脱臼", "伤到骨头"],
    "乳腺胀痛":  ["乳房疼", "乳腺疼", "乳晕疼", "乳房胀", "乳腺增生"],
    "胳膊疼":    ["手臂疼", "手臂痛", "胳膊酸", "手疼", "手痛"],
    "嘴疼":      ["嘴痛", "嘴巴疼", "口腔溃疡", "嘴唇疼", "口腔炎", "口痛"],
}

DEPARTMENT_NODES = [
    "口腔科", "呼吸内科", "消化内科", "心内科", "皮肤科",
    "眼科", "耳鼻喉科", "儿科", "骨科", "妇科", "神经内科",
    "急诊科", "乳腺外科", "胸外科",
]

# Map JSON cure_department values to Neo4j Department node names
DEPT_NORMALIZE: dict[str, str] = {
    "内科":       None,   # too vague — skip
    "外科":       None,
    "小儿内科":   "儿科",
    "小儿外科":   "儿科",
    "妇产科":     "妇科",
    "乳腺科":     "乳腺外科",
    "神经科":     "神经内科",
    "呼吸科":     "呼吸内科",
    "消化科":     "消化内科",
    "心血管内科": "心内科",
    "心脏内科":   "心内科",
    "心胸外科":   "胸外科",
    "耳鼻咽喉科": "耳鼻喉科",
    "五官科":     "耳鼻喉科",
    "口腔":       "口腔科",
    "皮肤":       "皮肤科",
    "骨科":       "骨科",
    "眼科":       "眼科",
}

# ── Helpers ──────────────────────────────────────────────────────────────


def _build_symptom_index() -> dict[str, str]:
    """Build a lookup: any alias text → canonical symptom name."""
    index: dict[str, str] = {}
    for canonical, aliases in SYMPTOM_ALIASES.items():
        index[canonical] = canonical
        for a in aliases:
            index[a] = canonical
    return index


def _build_dept_index() -> dict[str, str]:
    """Build a lookup: department text → canonical Neo4j department name."""
    index: dict[str, str] = {}
    for d in DEPARTMENT_NODES:
        index[d] = d
    for raw, normalized in DEPT_NORMALIZE.items():
        if normalized is not None:
            index[raw] = normalized
    return index


def match_symptom(text: str, symptom_index: dict[str, str]) -> str | None:
    """Return the canonical symptom name if text matches a known symptom."""
    # Exact match
    if text in symptom_index:
        return symptom_index[text]
    # Substring match
    for key, canonical in symptom_index.items():
        if len(key) >= 2 and (key in text or text in key):
            return canonical
    return None


def match_department(text: str, dept_index: dict[str, str]) -> str | None:
    """Return the canonical department name if text matches a known department."""
    if text in dept_index:
        return dept_index[text]
    # Substring / fuzzy
    for key, canonical in dept_index.items():
        if len(key) >= 2 and (key in text or text in key):
            return canonical
    return None


def clean_text(text: str | list[str] | None, max_len: int = 300) -> str:
    if text is None:
        return ""
    if isinstance(text, list):
        text = ", ".join(str(t) for t in text)
    text = text.replace("\\", "\\\\").replace('"', "'").replace("\n", " ").replace("\r", " ")
    text = text.replace(";", ",")   # semicolons break Cypher statement splitting
    text = re.sub(r"\s+", " ", text).strip()
    if len(text) > max_len:
        text = text[:max_len] + "..."
    return text


# ── Main ─────────────────────────────────────────────────────────────────


def load_all_diseases() -> dict[str, dict]:
    diseases: dict[str, dict] = {}
    if not MEDICAL_JSON.exists():
        print(f"ERROR: {MEDICAL_JSON} not found")
        return diseases
    with open(MEDICAL_JSON, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
                name = obj.get("name", "").strip()
                if not name:
                    continue
                diseases[name] = obj
            except Exception:
                pass
    return diseases


def build_cypher(diseases: dict[str, dict]) -> str:
    symptom_index = _build_symptom_index()
    dept_index = _build_dept_index()

    lines: list[str] = []
    lines.append("// ============================================================")
    lines.append("// Disease layer — auto-generated from medical.json")
    lines.append(f"// {len(diseases)} diseases total")
    lines.append("// ============================================================")
    lines.append("")

    # ── Clean previous ──
    lines.append("// ── Clean previous Disease nodes ──")
    lines.append("MATCH (d:Disease) DETACH DELETE d;")
    lines.append("")

    # ── Disease nodes ──
    lines.append("// ── Disease nodes ──")
    for name, d in sorted(diseases.items()):
        escaped = name.replace('"', "'")
        desc = clean_text(d.get("desc", ""), 300)
        cause = clean_text(d.get("cause", ""), 350)
        drugs = clean_text(", ".join(d.get("common_drug", [])[:8]), 300)
        cure_way = clean_text(", ".join(d.get("cure_way", [])), 200)
        prevent = clean_text(d.get("prevent", ""), 300)
        do_eat = clean_text(", ".join(d.get("do_eat", [])[:8]), 200) if d.get("do_eat") else ""
        not_eat = clean_text(", ".join(d.get("not_eat", [])[:8]), 200) if d.get("not_eat") else ""
        check = clean_text(", ".join(d.get("check", [])[:5]), 200) if d.get("check") else ""

        lines.append(
            f'CREATE (:Disease {{name: "{escaped}", desc: "{desc}", cause: "{cause}", '
            f'drugs: "{drugs}", cure_way: "{cure_way}", prevent: "{prevent}", '
            f'do_eat: "{do_eat}", not_eat: "{not_eat}", check: "{check}"}});'
        )
    lines.append("")

    # ── Symptom → Disease (HAS_DISEASE) ──
    lines.append("// ── Symptom → Disease (HAS_DISEASE) ──")
    symptom_disease_pairs: set[tuple[str, str]] = set()
    for name, d in diseases.items():
        symptoms = d.get("symptom", [])
        if not isinstance(symptoms, list):
            continue
        for sym_text in symptoms:
            canonical = match_symptom(str(sym_text).strip(), symptom_index)
            if canonical:
                symptom_disease_pairs.add((canonical, name))
    for sym, disease_name in sorted(symptom_disease_pairs):
        escaped_disease = disease_name.replace('"', "'")
        lines.append(
            f'MATCH (s:Symptom {{name: "{sym}"}}), (d:Disease {{name: "{escaped_disease}"}})'
        )
        lines.append("CREATE (s)-[:HAS_DISEASE]->(d);")
    lines.append("")

    # ── Disease → Department (BELONGS_TO) ──
    lines.append("// ── Disease → Department (BELONGS_TO) ──")
    disease_dept_pairs: set[tuple[str, str]] = set()
    for name, d in diseases.items():
        departments = d.get("cure_department", [])
        if not isinstance(departments, list):
            continue
        for dept_text in departments:
            canonical = match_department(str(dept_text).strip(), dept_index)
            if canonical:
                disease_dept_pairs.add((name, canonical))
    for disease_name, dept in sorted(disease_dept_pairs):
        escaped_disease = disease_name.replace('"', "'")
        lines.append(
            f'MATCH (d:Disease {{name: "{escaped_disease}"}}), (dept:Department {{name: "{dept}"}})'
        )
        lines.append("CREATE (d)-[:BELONGS_TO]->(dept);")
    lines.append("")

    return "\n".join(lines)


def main():
    diseases = load_all_diseases()
    if not diseases:
        print("No diseases loaded — check MEDICAL_JSON path.")
        return

    symptom_index = _build_symptom_index()
    dept_index = _build_dept_index()

    matched_symptom = 0
    matched_dept = 0
    for name, d in diseases.items():
        symptoms = d.get("symptom", [])
        if isinstance(symptoms, list):
            for s in symptoms:
                if match_symptom(str(s).strip(), symptom_index):
                    matched_symptom += 1
        departments = d.get("cure_department", [])
        if isinstance(departments, list):
            for dep in departments:
                if match_department(str(dep).strip(), dept_index):
                    matched_dept += 1

    print(f"Loaded {len(diseases)} diseases from medical.json")
    print(f"  HAS_DISEASE pairs:    {matched_symptom}")
    print(f"  BELONGS_TO pairs:     {matched_dept}")

    cypher = build_cypher(diseases)
    OUTPUT_CYPHER.write_text(cypher, encoding="utf-8")
    print(f"\nCypher written to: {OUTPUT_CYPHER}")

    # Also write disease names for rules.py to load
    disease_names = sorted(diseases.keys())
    OUTPUT_DISEASE_NAMES.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_DISEASE_NAMES.write_text("\n".join(disease_names), encoding="utf-8")
    print(f"Disease names written to: {OUTPUT_DISEASE_NAMES} ({len(disease_names)} names)")


if __name__ == "__main__":
    main()
