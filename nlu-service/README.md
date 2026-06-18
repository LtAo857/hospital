# Hospital NLU Inference

The NLU service for the hospital registration system.
Provides intent classification and slot extraction via HTTP API.

## Layout

```text
nlu-service/
  app.py
  requirements.txt
  server_stdlib.py
  hospital_nlu/
    parser.py
    rules.py
    constants.py
    neo4j.py
    langchain_demo.py
    langgraph_demo.py
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
cd nlu-service
pip install -r requirements.txt
uvicorn app:app --host 127.0.0.1 --port 8001 --reload
```

No-dependency standard-library version:

```powershell
cd nlu-service
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
  "source": "rules",
  "engine": "local-rule",
  "model": "hospital-nlu-v1",
  "latencyMs": 1,
  "accelerations": [
    "structured-output",
    "business-fallback-ready"
  ]
}
```

## LLM Mode

Set environment variables to replace the rule engine with a real LLM:

```powershell
$env:LLM_ENABLED="true"
$env:LLM_API_KEY="your-dashscope-api-key"
$env:LLM_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
$env:LLM_MODEL="qwen-plus"
$env:LLM_TIMEOUT="5"
$env:LLM_MIN_CONFIDENCE="0.7"
```

Then start as usual:

```powershell
python server_stdlib.py --host 127.0.0.1 --port 8001
```

How it works:

- First call the LLM (OpenAI-compatible API) with a dynamic system prompt that injects the current department list
- If LLM returns valid JSON and confidence ≥ `LLM_MIN_CONFIDENCE`, use the LLM result directly
- **Intent override**: LLM returns `unknown`/`unsupported` but text contains registration keywords (挂/预约/号等) → auto-correct to `registration`
- **Department fallback**: Neo4j graph query → symptom map → text extraction, 3-level cascade ensures explicitly mentioned departments are not lost
- If LLM fails, times out, or returns low confidence → **automatically fall back to the rule engine**
- Rule engine: jieba tokenization + symptom synonym fuzzy matching (13 standard symptoms, 80+ colloquial variants)
- If neither engine can confidently identify intent → `confidence: 0.0`, Java Coordinator returns "NLU 服务暂不可用" with a manual registration redirect
- The API response format is identical either way — callers don't need to know which engine ran

Response metadata:

```json
{
  "source": "llm",           // or "rules" when fallback
  "engine": "llm-remote",    // or "local-rule" when fallback
  "model": "qwen-plus",      // the actual model used
  "confidence": 0.95
}
```

## Evaluate Without API Dependencies

The parser, evaluator, local benchmark, and `server_stdlib.py` use only the
Python standard library.

```powershell
cd nlu-service
python scripts/eval.py
python scripts/benchmark.py --mode local --requests 200
python server_stdlib.py --self-test
```

## Build Demo Model File

```powershell
cd nlu-service
python scripts/train.py
```

This is not real LLM training. It builds the small local demo model file from
the sample data so the interview demo can run on a normal machine. The real
fine-tuning plan is documented in `docs/fine-tune-plan.md`.
