from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict


ROOT = Path(__file__).resolve().parents[1]
SAMPLES_PATH = ROOT / "data" / "sft_samples.jsonl"
MODEL_PATH = ROOT / "model" / "intent_slot_model.json"


BASE_MODEL: Dict[str, Any] = {
    "name": "hospital-nlu-v1",
    "engine": "local-rule",
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


def main() -> None:
    model = dict(BASE_MODEL)
    departments = set(model["departments"])
    symptom_map = dict(model["symptomDepartmentMap"])

    with SAMPLES_PATH.open("r", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            row = json.loads(line)
            output = json.loads(row["output"])
            slots = output.get("slots", {})
            department = slots.get("department")
            symptom = slots.get("symptom")
            if department:
                departments.add(department)
            if symptom and department:
                symptom_map[symptom] = department

    model["departments"] = sorted(departments)
    model["symptomDepartmentMap"] = dict(sorted(symptom_map.items()))

    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    with MODEL_PATH.open("w", encoding="utf-8") as f:
        json.dump(model, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print(f"wrote {MODEL_PATH}")
    print(f"departments={len(model['departments'])}")
    print(f"symptom_mappings={len(model['symptomDepartmentMap'])}")


if __name__ == "__main__":
    main()

