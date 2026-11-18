#!/usr/bin/env python3
"""Bounded end-to-end live smoke test for the imported Kuwo Legado API source.

This intentionally verifies each stage independently: search -> book detail ->
TOC -> chapter content. It does not treat HTTP 200 alone as a usable result.
"""
import json
import urllib.parse
import urllib.request

COLLECTION_URL = "https://shuyuan.nyasama.cc/shuyuan/12f348d3510bac0e5f1b164274f0b136.json"
KEYWORD = "斗破苍穹"
MAX_BYTES = 2 * 1024 * 1024
TIMEOUT = 10


def request_json(url, headers):
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=TIMEOUT) as response:
        body = response.read(MAX_BYTES + 1)
        if len(body) > MAX_BYTES:
            raise RuntimeError("response exceeded 2 MiB limit")
        if response.status < 200 or response.status >= 300:
            raise RuntimeError(f"HTTP {response.status}")
        payload = json.loads(body.decode("utf-8-sig"))
        if not isinstance(payload, dict):
            raise RuntimeError("API response is not a JSON object")
        return payload


def require_object(value, stage):
    if not isinstance(value, dict):
        raise RuntimeError(f"{stage}: expected JSON object, got {type(value).__name__}")
    return value


def require_list(value, stage):
    if isinstance(value, dict):
        value = list(value.values())
    if not isinstance(value, list) or not value:
        raise RuntimeError(f"{stage}: no records in data field")
    return value


def main():
    req = urllib.request.Request(
        COLLECTION_URL,
        headers={"Accept-Encoding": "identity", "User-Agent": "Mozilla/5.0"},
    )
    with urllib.request.urlopen(req, timeout=TIMEOUT) as response:
        raw = response.read(8 * 1024 * 1024 + 1)
    if len(raw) > 8 * 1024 * 1024:
        raise RuntimeError("source collection exceeded 8 MiB limit")
    sources = json.loads(raw.decode("utf-8-sig"))
    source = next(
        (item for item in sources
         if item.get("bookSourceName") == "酷我小说" and item.get("enabled") is not False),
        None,
    )
    if source is None:
        raise RuntimeError("enabled 酷我小说 source was not found")

    base = source["bookSourceUrl"].rstrip("/") + "/"
    headers = json.loads(source.get("header") or "{}")
    if not isinstance(headers, dict):
        headers = {}
    headers.setdefault("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
    headers.setdefault("Accept", "application/json, text/plain, */*")

    search_path = source["searchUrl"].replace("{{key}}", urllib.parse.quote(KEYWORD, safe="")).replace("{{page}}", "1")
    search_url = urllib.parse.urljoin(base, search_path)
    search_payload = request_json(search_url, headers)
    search_rows = require_list(search_payload.get("data"), "search")
    candidate = next(
        (row for row in search_rows
         if isinstance(row, dict) and row.get("book_id") and row.get("title")),
        None,
    )
    if candidate is None:
        raise RuntimeError(f"search: {len(search_rows)} rows but none has book_id/title")
    book_id = str(candidate["book_id"])
    print(f"LIVE_E2E_SEARCH_OK: keyword={KEYWORD!r}, rows={len(search_rows)}, title={candidate['title']!r}, book_id={book_id}")

    detail_path = source["ruleSearch"]["bookUrl"].replace("{{$.book_id}}", urllib.parse.quote(book_id, safe=""))
    detail_url = urllib.parse.urljoin(base, detail_path)
    detail_payload = request_json(detail_url, headers)
    detail_data = detail_payload.get("data")
    detail_name = detail_payload.get("title")
    if not isinstance(detail_name, str) or not detail_name.strip():
        if isinstance(detail_data, dict):
            detail_name = detail_data.get("title")
    if not isinstance(detail_name, str) or not detail_name.strip():
        raise RuntimeError(f"detail: no book title; keys={list(detail_payload)[:20]}")
    print(f"LIVE_E2E_DETAIL_OK: title={detail_name!r}, keys={list(detail_payload)[:12]}")

    toc_path = source["ruleBookInfo"]["tocUrl"].replace("{{$.book_id}}", urllib.parse.quote(book_id, safe=""))
    toc_url = urllib.parse.urljoin(base, toc_path)
    toc_payload = request_json(toc_url, headers)
    toc_rows = require_list(toc_payload.get("data"), "TOC")
    chapter = next(
        (row for row in toc_rows
         if isinstance(row, dict) and row.get("chapter_id") and row.get("chapter_title")),
        None,
    )
    if chapter is None:
        raise RuntimeError(f"TOC: {len(toc_rows)} rows but no chapter_id/chapter_title")
    chapter_id = str(chapter["chapter_id"])
    print(f"LIVE_E2E_TOC_OK: chapters={len(toc_rows)}, first_chapter={chapter.get('chapter_title')!r}")

    content_path = (source["ruleToc"]["chapterUrl"]
                    .replace("{{$.book_id}}", urllib.parse.quote(book_id, safe=""))
                    .replace("{{$.chapter_id}}", urllib.parse.quote(chapter_id, safe="")))
    content_url = urllib.parse.urljoin(base, content_path)
    content_payload = request_json(content_url, headers)
    data = content_payload.get("data")
    content = data.get("content") if isinstance(data, dict) else None
    if not isinstance(content, str) or not content.strip():
        raise RuntimeError(f"content: empty $.data.content; keys={list(content_payload)[:20]}")
    print(f"LIVE_E2E_CONTENT_OK: chapter={chapter.get('chapter_title')!r}, chars={len(content)}, preview={content[:80]!r}")
    print("LIVE_E2E_PASS: search -> detail -> TOC -> content all returned usable data")


if __name__ == "__main__":
    main()
