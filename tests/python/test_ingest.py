#!/usr/bin/env python3
import io
import json
import sys
import time
import urllib.request


def post_multipart(url: str, field_name: str, filename: str, content_bytes: bytes, content_type: str = "text/plain"):
    boundary = f"----KragBoundary{int(time.time() * 1000)}"
    body = io.BytesIO()
    body.write(f"--{boundary}\r\n".encode("utf-8"))
    body.write(
        (
            f"Content-Disposition: form-data; name=\"{field_name}\"; filename=\"{filename}\"\r\n"
            f"Content-Type: {content_type}\r\n\r\n"
        ).encode("utf-8")
    )
    body.write(content_bytes)
    body.write(f"\r\n--{boundary}--\r\n".encode("utf-8"))
    data = body.getvalue()

    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
    req.add_header("Content-Length", str(len(data)))
    with urllib.request.urlopen(req, timeout=15) as resp:
        status = resp.getcode()
        payload = resp.read().decode("utf-8")
        return status, payload


def main() -> int:
    url = "http://localhost:8080/api/v1/ingest/txt?tenantId=tenant1&kbId=kb1"
    content = b"This is a small text for pre-commit ingestion test.\nIt should produce vector embeddings.\n"
    try:
        status, payload = post_multipart(url, "file", "precommit.txt", content)
    except Exception as e:
        print(f"[python-test] Request failed: {e}", file=sys.stderr)
        return 1

    if status != 200:
        print(f"[python-test] Unexpected HTTP status: {status}", file=sys.stderr)
        print(payload)
        return 1

    try:
        data = json.loads(payload)
    except Exception as e:
        print(f"[python-test] Failed to parse JSON: {e} | payload={payload}", file=sys.stderr)
        return 1

    # Basic shape checks
    if not isinstance(data, dict):
        print(f"[python-test] Response is not a JSON object: {data}", file=sys.stderr)
        return 1

    required_keys = ["docId", "chunks", "dimension"]
    for k in required_keys:
        if k not in data:
            print(f"[python-test] Missing key '{k}' in response: {data}", file=sys.stderr)
            return 1

    if not isinstance(data.get("dimension"), int) or data.get("dimension") <= 0:
        print(f"[python-test] Invalid 'dimension' value: {data.get('dimension')}", file=sys.stderr)
        return 1

    if not isinstance(data.get("chunks"), int) or data.get("chunks") <= 0:
        print(f"[python-test] Invalid 'chunks' value: {data.get('chunks')}", file=sys.stderr)
        return 1

    print("[python-test] Ingestion test passed:", json.dumps(data))
    return 0


if __name__ == "__main__":
    sys.exit(main())