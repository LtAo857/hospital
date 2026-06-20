from __future__ import annotations

import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any, Dict

from hospital_nlu.parser import IntentSlotParser


parser = IntentSlotParser()


class Handler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        if self.path == "/health":
            self._send_json({"status": "ok"})
            return
        self._send_json({"error": "not_found"}, status=404)

    def do_POST(self) -> None:
        if self.path != "/infer":
            self._send_json({"error": "not_found"}, status=404)
            return

        try:
            length = int(self.headers.get("Content-Length", "0"))
            raw = self.rfile.read(length)
            try:
                body = raw.decode("utf-8")
            except UnicodeDecodeError:
                body = raw.decode("gbk")
            print(f"[infer] body={body}")
            payload = json.loads(body) if body else {}
            text = str(payload.get("text", "")).strip()
            if not text:
                self._send_json({"error": "text is required"}, status=400)
                return
            departments = payload.get("departments")
            result = parser.parse(text, departments)
            print(f"[infer] result={json.dumps(result, ensure_ascii=False)}")
            self._send_json(result)
        except json.JSONDecodeError:
            self._send_json({"error": "invalid_json"}, status=400)

    def log_message(self, format: str, *args: Any) -> None:
        return

    def _send_json(self, payload: Dict[str, Any], status: int = 200) -> None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def main() -> None:
    args = parse_args()
    if args.self_test:
        print(json.dumps(parser.parse(args.text), ensure_ascii=False, indent=2))
        return

    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"serving http://{args.host}:{args.port}")
    server.serve_forever()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8001)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--text", default="明天牙疼，帮我挂个口腔科的号")
    return parser.parse_args()


if __name__ == "__main__":
    main()

