#!/usr/bin/env python3
"""Live search -> detail -> TOC -> content smoke for the maintained Pinshuzhai source."""
import http.client
import json
import socket
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request

SOURCE_URL = "https://raw.githubusercontent.com/yuyuan-cc/shuyuan/faec02099a6671d3fdeb28dda25eb7d59c08bae0/50.json"
KEYWORDS = ("斗破苍穹", "凡人修仙传", "道君")
MAX_BYTES = 3 * 1024 * 1024
TIMEOUT = 25
ATTEMPTS = 3


def retryable(exc):
    if isinstance(exc, urllib.error.HTTPError):
        return exc.code in (408, 425, 429) or 500 <= exc.code <= 599
    if isinstance(exc, urllib.error.URLError):
        reason = exc.reason
        if isinstance(reason, ssl.SSLError):
            return "timed out" in str(reason).lower() or "want_read" in str(reason).lower()
        return isinstance(reason, (TimeoutError, socket.timeout, socket.gaierror,
                                   ConnectionResetError, ConnectionAbortedError,
                                   ConnectionRefusedError, http.client.RemoteDisconnected))
    if isinstance(exc, ssl.SSLError):
        return "timed out" in str(exc).lower() or "want_read" in str(exc).lower()
    return isinstance(exc, (TimeoutError, socket.timeout, ConnectionResetError,
                            ConnectionAbortedError, http.client.RemoteDisconnected))


def fetch(url, headers):
    req = urllib.request.Request(url, headers=headers)
    for attempt in range(1, ATTEMPTS + 1):
        started = time.monotonic()
        try:
            with urllib.request.urlopen(req, timeout=TIMEOUT) as response:
                headers_at = time.monotonic()
                body = response.read(MAX_BYTES + 1)
                finished = time.monotonic()
                if len(body) > MAX_BYTES:
                    raise RuntimeError(f"response exceeded {MAX_BYTES} byte limit: {url}")
                if response.status < 200 or response.status >= 300:
                    raise RuntimeError(f"HTTP {response.status}: {url}")
                print(
                    f"LIVE_HTTP_TIMING host={urllib.parse.urlsplit(url).hostname} "
                    f"attempt={attempt}/{ATTEMPTS} request_to_headers={headers_at-started:.2f}s "
                    f"body={finished-headers_at:.2f}s total={finished-started:.2f}s "
                    f"status={response.status} bytes={len(body)}",
                    flush=True,
                )
                return response.geturl(), body
        except Exception as exc:
            elapsed = time.monotonic() - started
            again = attempt < ATTEMPTS and retryable(exc)
            print(
                f"LIVE_HTTP_ATTEMPT_FAIL host={urllib.parse.urlsplit(url).hostname} "
                f"attempt={attempt}/{ATTEMPTS} elapsed={elapsed:.2f}s "
                f"retry={again} error={type(exc).__name__}: {exc}",
                flush=True,
            )
            if not again:
                raise
            time.sleep(min(float(attempt), 3.0))


def main():
    from bs4 import BeautifulSoup

    stage = "source_descriptor"
    try:
        _, raw = fetch(SOURCE_URL, {"User-Agent": "Badcat-Legado-Compatibility-Test/1.0"})
        payload = json.loads(raw.decode("utf-8-sig"))
        source = next((x for x in (payload if isinstance(payload, list) else [payload])
                       if isinstance(x, dict) and x.get("bookSourceName") == "品书斋（优）"), None)
        if source is None:
            raise RuntimeError("maintained 品书斋（优） source descriptor not found")
        base = source["bookSourceUrl"].strip()
        headers = json.loads(source.get("header") or "{}")
        headers.setdefault("User-Agent", "Mozilla/5.0 (Linux; Android 8.1.0) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
        headers.setdefault("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

        stage = "search"
        candidate = None
        for keyword in KEYWORDS:
            path = source["searchUrl"].replace("{{page}}", "1").replace(
                "{{key}}", urllib.parse.quote(keyword, safe="")
            )
            search_url = urllib.parse.urljoin(base.rstrip("/") + "/", path)
            final_url, body = fetch(search_url, headers)
            soup = BeautifulSoup(body, "html.parser")
            nodes = soup.select(source["ruleSearch"]["bookList"])
            print(f"LIVE_PINSHUZHAI_SEARCH_ATTEMPT: keyword={keyword!r}, rows={len(nodes)}, bytes={len(body)}, final_url={final_url}", flush=True)
            for node in nodes:
                anchor = node.select_one(".bookinfo h4 a")
                if anchor and anchor.get_text(" ", strip=True) and anchor.get("href"):
                    candidate = (anchor.get_text(" ", strip=True), urllib.parse.urljoin(final_url, anchor["href"]))
                    break
            if candidate:
                print(f"LIVE_PINSHUZHAI_SEARCH_OK: title={candidate[0]!r}, book_url={candidate[1]}", flush=True)
                break
        if not candidate:
            raise RuntimeError("search returned no list item with both title and book URL")

        stage = "detail"
        title, book_url = candidate
        detail_url, detail_body = fetch(book_url, headers)
        detail = BeautifulSoup(detail_body, "html.parser")
        og_title = detail.select_one('meta[property="og:novel:book_name"]')
        detail_title = (og_title.get("content", "").strip() if og_title else "") or (detail.title.get_text(" ", strip=True) if detail.title else "")
        if not detail_title or len(detail_body) < 300:
            raise RuntimeError(f"detail unusable: title={detail_title!r}, bytes={len(detail_body)}")
        print(f"LIVE_PINSHUZHAI_DETAIL_OK: search_title={title!r}, detail_title={detail_title!r}, bytes={len(detail_body)}", flush=True)

        stage = "toc"
        toc_nodes = detail.select(source["ruleToc"]["chapterList"])
        chapters = [(a.get_text(" ", strip=True), urllib.parse.urljoin(detail_url, a.get("href", "")))
                    for a in toc_nodes if a.get("href") and a.get_text(" ", strip=True)]
        if not chapters:
            raise RuntimeError(f"TOC empty: selector={source['ruleToc']['chapterList']!r}, detail_url={detail_url}")
        chapter_title, chapter_url = chapters[0]
        print(f"LIVE_PINSHUZHAI_TOC_OK: chapters={len(chapters)}, first={chapter_title!r}, url={chapter_url}", flush=True)

        stage = "content"
        content_url, content_body = fetch(chapter_url, headers)
        content_doc = BeautifulSoup(content_body, "html.parser")
        wrapper = content_doc.select_one(".tjlm + div") or content_doc.select_one(
            ".readcons, .duboxs, .chapterbo, .chapterbox"
        )
        if wrapper is None:
            raise RuntimeError(f"content container missing: bytes={len(content_body)}, url={content_url}")
        for junk in wrapper.select(".ydgong, article.hid, article.shid, .shid, .shidp, script, style"):
            junk.decompose()
        text = wrapper.get_text("\\n", strip=True)
        if len(text) < 80:
            raise RuntimeError(f"content too short ({len(text)} chars): {text[:160]!r}")
        print(f"LIVE_PINSHUZHAI_CONTENT_OK: chapter={chapter_title!r}, chars={len(text)}, preview={text[:100]!r}", flush=True)
        print("LIVE_PINSHUZHAI_E2E_PASS: search -> detail -> TOC -> content", flush=True)
    except Exception as exc:
        print(f"LIVE_PINSHUZHAI_E2E_PARTIAL: failed_stage={stage}, error={type(exc).__name__}: {exc}", flush=True)
        raise


if __name__ == "__main__":
    main()
