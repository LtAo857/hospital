from __future__ import annotations

import argparse
import json
import statistics
import sys
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any, Dict, List


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from hospital_nlu.parser import IntentSlotParser  # noqa: E402


DEFAULT_TEXTS = [
    "明天牙疼，帮我挂个口腔科的号",
    "后天上午想挂呼吸内科",
    "我咳嗽发烧，明天能挂哪个科",
    "张医生明天还有号吗",
    "看看我的消息",
    "查一下我的就诊卡",
    "为什么推荐这个医生",
]


def main() -> None:
    args = parse_args()
    texts = [DEFAULT_TEXTS[i % len(DEFAULT_TEXTS)] for i in range(args.requests)]

    started_at = time.perf_counter()
    latencies: List[float] = []

    if args.mode == "http":
        worker = lambda text: infer_http(args.endpoint, text)
    else:
        parser = IntentSlotParser()
        worker = lambda text: infer_local(parser, text)

    with ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        futures = [executor.submit(worker, text) for text in texts]
        for future in as_completed(futures):
            latencies.append(future.result())

    elapsed = time.perf_counter() - started_at
    result = {
        "mode": args.mode,
        "requests": args.requests,
        "concurrency": args.concurrency,
        "totalSeconds": round(elapsed, 4),
        "qps": round(args.requests / elapsed, 2) if elapsed else 0,
        "avgLatencyMs": round(statistics.mean(latencies), 2),
        "p95LatencyMs": round(percentile(latencies, 95), 2),
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))


def infer_local(parser: IntentSlotParser, text: str) -> float:
    started_at = time.perf_counter()
    parser.parse(text)
    return (time.perf_counter() - started_at) * 1000


def infer_http(endpoint: str, text: str) -> float:
    payload = json.dumps({"text": text}).encode("utf-8")
    request = urllib.request.Request(
        endpoint,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    started_at = time.perf_counter()
    with urllib.request.urlopen(request, timeout=5) as response:
        response.read()
    return (time.perf_counter() - started_at) * 1000


def percentile(values: List[float], p: int) -> float:
    ordered = sorted(values)
    if not ordered:
        return 0.0
    index = int(round((p / 100) * (len(ordered) - 1)))
    return ordered[index]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=["local", "http"], default="local")
    parser.add_argument("--endpoint", default="http://127.0.0.1:8001/infer")
    parser.add_argument("--requests", type=int, default=100)
    parser.add_argument("--concurrency", type=int, default=8)
    return parser.parse_args()


if __name__ == "__main__":
    main()

