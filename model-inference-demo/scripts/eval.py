from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any, Dict, List


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from inference_demo.parser import IntentSlotParser  # noqa: E402


EVAL_PATH = ROOT / "data" / "eval.jsonl"


def main() -> None:
    parser = IntentSlotParser()
    rows = load_jsonl(EVAL_PATH)

    intent_correct = 0
    slot_expected = 0
    slot_correct = 0
    bad_cases: List[Dict[str, Any]] = []

    for row in rows:
        predicted = parser.parse(row["text"])
        expected = row["expected"]

        intent_ok = predicted["intent"] == expected["intent"]
        if intent_ok:
            intent_correct += 1

        slot_errors = []
        for key, expected_value in expected.get("slots", {}).items():
            slot_expected += 1
            actual_value = predicted["slots"].get(key)
            if actual_value == expected_value:
                slot_correct += 1
            else:
                slot_errors.append(
                    {"slot": key, "expected": expected_value, "actual": actual_value}
                )

        if not intent_ok or slot_errors:
            bad_cases.append(
                {
                    "text": row["text"],
                    "expected": expected,
                    "predicted": {
                        "intent": predicted["intent"],
                        "slots": predicted["slots"],
                        "confidence": predicted["confidence"],
                    },
                    "slotErrors": slot_errors,
                }
            )

    intent_accuracy = intent_correct / len(rows) if rows else 0.0
    slot_accuracy = slot_correct / slot_expected if slot_expected else 1.0

    print(json.dumps(
        {
            "samples": len(rows),
            "intentAccuracy": round(intent_accuracy, 4),
            "slotAccuracy": round(slot_accuracy, 4),
            "badCaseCount": len(bad_cases),
            "badCases": bad_cases,
        },
        ensure_ascii=False,
        indent=2,
    ))


def load_jsonl(path: Path) -> List[Dict[str, Any]]:
    rows = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                rows.append(json.loads(line))
    return rows


if __name__ == "__main__":
    main()

