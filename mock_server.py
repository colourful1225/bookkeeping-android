#!/usr/bin/env python3
"""
本地 Mock 后端服务器（无需 Android Studio / 真实后端）。

用途：为 Android debug 包提供 /v1/transactions/upsert 接口，
     模拟服务端返回 serverId，验证 PENDING → SYNCED 同步链路。

运行：
    python mock_server.py
    # 默认监听 0.0.0.0:8080

模拟器访问地址：  http://10.0.2.2:8080
真机访问地址：    http://<电脑局域网IP>:8080

将上述地址填入 app/build.gradle.kts 的 debug.buildConfigField("BASE_URL")
并将 USE_MOCK_API 改为 false（改用真实网络请求）。
"""

import json
import uuid
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

# 幂等存储：idempotency_key -> server_id
_idempotency_store: dict[str, str] = {}


class MockHandler(BaseHTTPRequestHandler):

    def log_message(self, fmt, *args):  # 覆盖默认 log，加时间戳
        print(f"[{time.strftime('%H:%M:%S')}] {fmt % args}")

    def do_POST(self):
        if self.path == "/v1/transactions/upsert":
            self._handle_upsert()
        else:
            self._send_json(404, {"error": f"unknown path: {self.path}"})

    def _handle_upsert(self):
        length = int(self.headers.get("Content-Length", 0))
        body   = json.loads(self.rfile.read(length)) if length else {}

        idempotency_key = self.headers.get("Idempotency-Key", "")
        tx_id           = body.get("id", "unknown")

        # 幂等：相同 key 返回相同 server_id
        if idempotency_key and idempotency_key in _idempotency_store:
            server_id = _idempotency_store[idempotency_key]
            print(f"  ↩ 幂等命中 key={idempotency_key} server_id={server_id}")
        else:
            server_id = f"srv-{uuid.uuid4()}"
            if idempotency_key:
                _idempotency_store[idempotency_key] = server_id
            print(f"  ✅ 新建 tx_id={tx_id} → server_id={server_id}")

        self._send_json(200, {"server_id": server_id})

    def _send_json(self, code: int, data: dict):
        payload = json.dumps(data).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


if __name__ == "__main__":
    HOST, PORT = "0.0.0.0", 8080
    server = HTTPServer((HOST, PORT), MockHandler)
    print(f"Mock 后端启动：http://{HOST}:{PORT}")
    print(f"  模拟器访问：http://10.0.2.2:{PORT}")
    print(f"  按 Ctrl+C 停止\n")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n已停止")
