# Model Inference Demo

This is an independent interview demo for the hospital registration project.
It shows how fine-tuning and inference acceleration can be introduced without
making the main registration workflow depend on a model.

The demo focuses on NLU:

- intent classification
- slot extraction
- structured JSON output
- evaluation and benchmark scripts
- fine-tuning and accelerated inference design docs

The production boundary is intentional: the model only understands user text.
Real doctors, schedules, cards, duplicate registration checks, and final writes
must still come from the Java business system and MySQL.

## Layout

```text
model-inference-demo/
  app.py
  requirements.txt
  inference_demo/
    parser.py
  data/
    sft_samples.jsonl
    eval.jsonl
  model/
    intent_slot_model.json
  scripts/
    train.py
    eval.py
    benchmark.py
  docs/
    fine-tune-plan.md
    inference-acceleration.md
    integration-with-hospital.md
```

## Run API

FastAPI version:

```powershell
cd model-inference-demo
pip install -r requirements.txt
uvicorn app:app --host 127.0.0.1 --port 8001 --reload
```

No-dependency standard-library version:

```powershell
cd model-inference-demo
python server_stdlib.py --host 127.0.0.1 --port 8001
```

Request:

```http
POST http://127.0.0.1:8001/infer
Content-Type: application/json

{
  "text": "明天牙疼，帮我挂个口腔科的号",
  "sessionId": "demo-session"
}
```

Response:

```json
{
  "intent": "registration",
  "slots": {
    "symptom": "牙疼",
    "department": "口腔科",
    "doctorName": null,
    "date": "明天",
    "timePreference": null
  },
  "confidence": 0.95,
  "source": "demo_rules",
  "engine": "local-rule-demo",
  "model": "hospital-nlu-demo-v1",
  "latencyMs": 1,
  "accelerations": [
    "structured-output",
    "business-fallback-ready"
  ]
}
```

## Evaluate Without API Dependencies

The parser, evaluator, local benchmark, and `server_stdlib.py` use only the
Python standard library.

```powershell
cd model-inference-demo
python scripts/eval.py
python scripts/benchmark.py --mode local --requests 200
python server_stdlib.py --self-test
```

## Build Demo Model File

```powershell
cd model-inference-demo
python scripts/train.py
```

This is not real LLM training. It builds the small local demo model file from
the sample data so the interview demo can run on a normal machine. The real
fine-tuning plan is documented in `docs/fine-tune-plan.md`.
