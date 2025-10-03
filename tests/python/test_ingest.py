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

def post_text_plain(url: str, content_bytes: bytes):
    req = urllib.request.Request(url, data=content_bytes, method="POST")
    req.add_header("Content-Type", "text/plain")
    req.add_header("Content-Length", str(len(content_bytes)))
    with urllib.request.urlopen(req, timeout=15) as resp:
        status = resp.getcode()
        payload = resp.read().decode("utf-8")
        return status, payload


def main() -> int:
    content = b"This is a small text for pre-commit ingestion test.\nIt should produce vector embeddings.\n"

    # Test multipart /ingest/txt
    url_file = "http://localhost:8080/api/v1/ingest/txt?tenantId=tenant1&kbId=kb1"
    try:
        status_file, payload_file = post_multipart(url_file, "file", "precommit.txt", content)
    except Exception as e:
        print(f"[python-test] /ingest/txt request failed: {e}", file=sys.stderr)
        return 1

    if status_file != 200:
        print(f"[python-test] /ingest/txt unexpected HTTP status: {status_file}", file=sys.stderr)
        print(payload_file)
        return 1

    try:
        data_file = json.loads(payload_file)
    except Exception as e:
        print(f"[python-test] /ingest/txt failed to parse JSON: {e} | payload={payload_file}", file=sys.stderr)
        return 1

    if not isinstance(data_file, dict):
        print(f"[python-test] /ingest/txt response is not a JSON object: {data_file}", file=sys.stderr)
        return 1

    required_keys = ["docId", "chunks", "dimension"]
    for k in required_keys:
        if k not in data_file:
            print(f"[python-test] /ingest/txt missing key '{k}' in response: {data_file}", file=sys.stderr)
            return 1

    if not isinstance(data_file.get("dimension"), int) or data_file.get("dimension") <= 0:
        print(f"[python-test] /ingest/txt invalid 'dimension' value: {data_file.get('dimension')}", file=sys.stderr)
        return 1

    if not isinstance(data_file.get("chunks"), int) or data_file.get("chunks") <= 0:
        print(f"[python-test] /ingest/txt invalid 'chunks' value: {data_file.get('chunks')}", file=sys.stderr)
        return 1

    print("[python-test] /ingest/txt passed:", json.dumps(data_file))

    # Test text/plain /ingest/text
    url_text = "http://localhost:8080/api/v1/ingest/text?tenantId=tenant1&kbId=kb1&filename=inline.txt"
    try:
        status_text, payload_text = post_text_plain(url_text, content)
    except Exception as e:
        print(f"[python-test] /ingest/text request failed: {e}", file=sys.stderr)
        return 1

    if status_text != 200:
        print(f"[python-test] /ingest/text unexpected HTTP status: {status_text}", file=sys.stderr)
        print(payload_text)
        return 1

    try:
        data_text = json.loads(payload_text)
    except Exception as e:
        print(f"[python-test] /ingest/text failed to parse JSON: {e} | payload={payload_text}", file=sys.stderr)
        return 1

    if not isinstance(data_text, dict):
        print(f"[python-test] /ingest/text response is not a JSON object: {data_text}", file=sys.stderr)
        return 1

    for k in required_keys:
        if k not in data_text:
            print(f"[python-test] /ingest/text missing key '{k}' in response: {data_text}", file=sys.stderr)
            return 1

    if not isinstance(data_text.get("dimension"), int) or data_text.get("dimension") <= 0:
        print(f"[python-test] /ingest/text invalid 'dimension' value: {data_text.get('dimension')}", file=sys.stderr)
        return 1

    if not isinstance(data_text.get("chunks"), int) or data_text.get("chunks") <= 0:
        print(f"[python-test] /ingest/text invalid 'chunks' value: {data_text.get('chunks')}", file=sys.stderr)
        return 1

    print("[python-test] /ingest/text passed:", json.dumps(data_text))
    return 0


if __name__ == "__main__":
    sys.exit(main())