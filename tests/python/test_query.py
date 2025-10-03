#!/usr/bin/env python3
import json
import sys
import urllib.request
import urllib.error


def post_json(url: str, obj: dict):
    data = json.dumps(obj).encode("utf-8")
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
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

    # Ensure we have a document to query: ingest via text/plain
    url_ingest = "http://localhost:8080/api/v1/ingest/text?tenantId=tenant1&kbId=kb1&filename=querycase.txt"
    try:
        status_ing, payload_ing = post_text_plain(url_ingest, content)
    except Exception as e:
        print(f"[python-test] /ingest/text request failed: {e}", file=sys.stderr)
        return 1

    if status_ing != 200:
        print(f"[python-test] /ingest/text unexpected HTTP status: {status_ing}", file=sys.stderr)
        print(payload_ing)
        return 1

    try:
        data_ing = json.loads(payload_ing)
    except Exception as e:
        print(f"[python-test] /ingest/text failed to parse JSON: {e} | payload={payload_ing}", file=sys.stderr)
        return 1

    if not isinstance(data_ing, dict):
        print(f"[python-test] /ingest/text response is not a JSON object: {data_ing}", file=sys.stderr)
        return 1

    for k in ["docId", "chunks", "dimension"]:
        if k not in data_ing:
            print(f"[python-test] /ingest/text missing key '{k}' in response: {data_ing}", file=sys.stderr)
            return 1

    doc_id = data_ing.get("docId")
    print("[python-test] /ingest/text prepared doc:", json.dumps({"docId": doc_id, "chunks": data_ing.get("chunks")}))

    # Query by text (vector search)
    url_query = "http://localhost:8080/api/v1/query"
    body_vec = {
        "tenantId": "tenant1",
        "kbId": "kb1",
        "query": "pre-commit ingestion",
        "topK": 5,
        "minScore": 0.0,
    }
    try:
        status_q1, payload_q1 = post_json(url_query, body_vec)
    except Exception as e:
        print(f"[python-test] /query (vector) request failed: {e}", file=sys.stderr)
        return 1

    if status_q1 != 200:
        print(f"[python-test] /query (vector) unexpected HTTP status: {status_q1}", file=sys.stderr)
        print(payload_q1)
        return 1

    try:
        data_q1 = json.loads(payload_q1)
    except Exception as e:
        print(f"[python-test] /query (vector) failed to parse JSON: {e} | payload={payload_q1}", file=sys.stderr)
        return 1

    if "results" not in data_q1 or not isinstance(data_q1["results"], list) or len(data_q1["results"]) == 0:
        print(f"[python-test] /query (vector) invalid results: {data_q1}", file=sys.stderr)
        return 1

    # Basic shape checks
    first = data_q1["results"][0]
    for k in ["docId", "chunkId", "text", "score"]:
        if k not in first:
            print(f"[python-test] /query (vector) missing key '{k}' in result item: {first}", file=sys.stderr)
            return 1

    # Ensure queried doc appears in results (not strictly required, but expected)
    if doc_id and not any(r.get("docId") == doc_id for r in data_q1["results"]):
        print(f"[python-test] /query (vector) docId {doc_id} not found in results", file=sys.stderr)
        return 1

    print("[python-test] /query (vector) passed:", json.dumps({"topK": len(data_q1["results"])}))

    # Query by docId with full=true
    body_doc = {
        "tenantId": "tenant1",
        "kbId": "kb1",
        "docId": doc_id,
        "full": True,
    }
    try:
        status_q2, payload_q2 = post_json(url_query, body_doc)
    except Exception as e:
        print(f"[python-test] /query (docId) request failed: {e}", file=sys.stderr)
        return 1

    if status_q2 != 200:
        print(f"[python-test] /query (docId) unexpected HTTP status: {status_q2}", file=sys.stderr)
        print(payload_q2)
        return 1

    try:
        data_q2 = json.loads(payload_q2)
    except Exception as e:
        print(f"[python-test] /query (docId) failed to parse JSON: {e} | payload={payload_q2}", file=sys.stderr)
        return 1

    if doc_id != data_q2.get("docId"):
        print(f"[python-test] /query (docId) mismatched docId: {data_q2.get('docId')} != {doc_id}", file=sys.stderr)
        return 1

    chunks = data_q2.get("chunks")
    if not isinstance(chunks, list) or len(chunks) == 0:
        print(f"[python-test] /query (docId) invalid chunks: {chunks}", file=sys.stderr)
        return 1

    doc_text = data_q2.get("documentText")
    if not isinstance(doc_text, str) or len(doc_text.strip()) == 0:
        print(f"[python-test] /query (docId) invalid documentText: {doc_text}", file=sys.stderr)
        return 1

    print("[python-test] /query (docId) passed:", json.dumps({"chunks": len(chunks), "textLen": len(doc_text)}))

    # Negative case: missing query when docId not provided -> expect 400
    body_bad = {"tenantId": "tenant1", "kbId": "kb1"}
    try:
        status_bad, payload_bad = post_json(url_query, body_bad)
        # If server responds 200, it's incorrect
        print(f"[python-test] /query (bad) expected 400, got {status_bad}", file=sys.stderr)
        print(payload_bad)
        return 1
    except urllib.error.HTTPError as he:
        if he.code != 400:
            print(f"[python-test] /query (bad) expected HTTP 400, got {he.code}", file=sys.stderr)
            return 1
        print("[python-test] /query (bad) passed: received HTTP 400 as expected")

    return 0


if __name__ == "__main__":
    sys.exit(main())