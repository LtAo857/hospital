# Inference Acceleration

## Recommended Route

For a real GPU deployment, use vLLM as the model serving layer and expose an
OpenAI-compatible API. The hospital system should not bind to vLLM directly; it
should call the stable `/infer` service.

```text
Java multi-agent
  -> HTTP /infer
  -> FastAPI adapter
  -> vLLM model server
  -> LoRA-adapted model output
```

## Why vLLM

vLLM is a good interview default because it is focused on LLM serving:

- high-throughput serving
- KV-cache oriented optimization
- continuous batching style serving
- OpenAI-compatible API shape
- reasonable integration story for LoRA-adapted models

## Local Demo Options

If the machine has no suitable GPU:

- Ollama: simplest local model server for demo.
- llama.cpp: good for GGUF quantized models and low-resource deployment.
- Local rule parser: current repository fallback so the demo remains runnable.

## What to Measure

Use `scripts/benchmark.py` for a small demo benchmark:

- average latency
- P95 latency
- QPS
- JSON response success rate, if using the model-backed service

Example:

```powershell
python scripts/benchmark.py --mode local --requests 200 --concurrency 8
python scripts/benchmark.py --mode http --endpoint http://127.0.0.1:8001/infer
```

## Production Guardrails

- set a short timeout, such as 800 ms to 1500 ms
- fall back to the rule parser on timeout or invalid JSON
- log prompt, response schema validity, latency, model version, and bad case type
- avoid model retries on write operations
- keep model output away from final registration writes

