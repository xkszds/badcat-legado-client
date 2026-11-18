#!/usr/bin/env python3
"""Live full-reading probes against several sanitized sources from the user's uploaded corpus.

Each candidate is tried independently. A network timeout is reported as a source
connectivity failure and does not prevent testing the next corpus source. No
private headers, cookies, login tokens, or JS signing secrets are included.
"""
import http.client
import re
import socket
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request

from bs4 import BeautifulSoup

MAX_BYTES = 3 * 1024 * 1024
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
}

# These descriptors are copied from the uploaded source file with authentication
# material deliberately omitted. They cover different common CSS rule shapes.
SOURCES = [
    {
        "name": "唐三中文", "base": "http://www.xtangsanshu.com",
        "search": "/s.php?ie=utf-8&s=&q={{key}}",
        "search_list": ".bookbox", "search_name": ".bookname@text", "search_url": "a.0@href",
        "detail_name": ".info@h2@text", "toc_list": ".listmain@dd@a",
        "toc_name": "text", "toc_url": "href", "content": "id.content@html",
    },
    {
        "name": "笔趣阁fun", "base": "https://www.bqg.fun",
        "search": "/search.php?searchkey={{key}}",
        "search_list": "tbody@tr!0", "search_name": "a.0@text", "search_url": "a.0@href",
        "detail_name": "id.info@h1@text", "toc_list": "id.list@dd@a",
        "toc_name": "text", "toc_url": "href", "content": "id.booktext@html",
    },
    {
        "name": "精华书阁", "base": "https://m.jhsssd.com",
        "search": "/search.html?ie=utf-8&word={{key}}&page=1",
        "search_list": ".searchbook", "search_name": ".bookname@text", "search_url": "a@href",
        "detail_name": "h2@text", "toc_list": "ul.1@a",
        "toc_name": "text", "toc_url": "href", "content": "id.nr1@html",
    },
    {
        "name": "零零小说", "base": "https://www.00shu.la",
        "search": "/modules/article/search.php?q={{key}}",
        "search_list": "tbody@tr!0", "search_name": "a.0@text", "search_url": "a.0@href",
        "detail_name": "id.info@h1@text", "toc_list": "id.list@dd@a",
        "toc_name": "text", "toc_url": "href", "content": "id.content@html",
    },
]

def retryable(exc):
    # Do not waste two more 25-second attempts on permanent 4xx responses.
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

def fetch(url, attempts=3, timeout=25):
    request = urllib.request.Request(url, headers=HEADERS)
    for attempt in range(attempts):
        started = time.monotonic()
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                headers_at = time.monotonic()
                body = response.read(MAX_BYTES + 1)
                finished = time.monotonic()
                if len(body) > MAX_BYTES:
                    raise RuntimeError(f"response exceeds {MAX_BYTES} bytes")
                if response.status < 200 or response.status >= 300:
                    raise RuntimeError(f"HTTP {response.status}")
                charset = response.headers.get_content_charset() or "utf-8"
                try:
                    html = body.decode(charset)
                except (LookupError, UnicodeDecodeError):
                    html = body.decode("utf-8", errors="replace")
                print(
                    f"HTTP_TIMING host={urllib.parse.urlsplit(url).hostname} "
                    f"attempt={attempt + 1}/{attempts} "
                    f"request_to_headers={headers_at - started:.2f}s "
                    f"body={finished - headers_at:.2f}s total={finished - started:.2f}s "
                    f"status={response.status} bytes={len(body)}",
                    flush=True,
                )
                return response.geturl(), html
        except Exception as exc:
            elapsed = time.monotonic() - started
            again = attempt + 1 < attempts and retryable(exc)
            print(
                f"HTTP_ATTEMPT_FAIL host={urllib.parse.urlsplit(url).hostname} "
                f"attempt={attempt + 1}/{attempts} elapsed={elapsed:.2f}s "
                f"retry={again} error={type(exc).__name__}: {exc}",
                flush=True,
            )
            if not again:
                raise
            time.sleep(min(1.0 * (2 ** attempt), 4.0))

def selector_part(part):
    part = part.strip()
    skip_index = None
    if "!" in part:
        part, raw_skip = part.rsplit("!", 1)
        try:
            skip_index = int(raw_skip)
        except ValueError:
            skip_index = None
    index = None
    match = re.search(r"\.(-?\d+)$", part)
    if match:
        index = int(match.group(1))
        part = part[:match.start()]
    if part.startswith("id."):
        part = "#" + part[3:]
    elif part.startswith("class."):
        part = "." + part[6:].replace(" ", ".")
    elif part.startswith("tag."):
        part = part[4:]
    return part or "*", index, skip_index

def select_rule(root, rule):
    current = [root]
    for part in [p.strip() for p in (rule or "").split("@") if p.strip()]:
        low = part.lower()
        if low in {"text", "textnodes", "owntext", "html", "href", "src", "content"}:
            break
        selector, index, skip_index = selector_part(part)
        selected = []
        for node in current:
            selected.extend(node.select(selector))
        if skip_index is not None:
            selected = [node for i, node in enumerate(selected) if i != skip_index]
        if index is not None:
            selected = [selected[index]] if -len(selected) <= index < len(selected) else []
        current = selected
    return current

def extract(node, rule):
    parts = [p.strip() for p in (rule or "").split("@") if p.strip()]
    current = [node]
    for part in parts:
        low = part.lower()
        if low in {"text", "textnodes", "owntext", "html", "href", "src", "content"}:
            if low == "html":
                return "".join(str(x) for x in current)
            if low in {"href", "src", "content"}:
                return next((x.get(low, "") for x in current if x.get(low)), "")
            if low == "owntext":
                return " ".join("".join(x.find_all(string=True, recursive=False)) for x in current).strip()
            return " ".join(x.get_text(" ", strip=True) for x in current).strip()
        selector, index, skip_index = selector_part(part)
        selected = []
        for item in current:
            selected.extend(item.select(selector))
        if skip_index is not None:
            selected = [item for i, item in enumerate(selected) if i != skip_index]
        if index is not None:
            selected = [selected[index]] if -len(selected) <= index < len(selected) else []
        current = selected
    return " ".join(x.get_text(" ", strip=True) for x in current).strip()

def valid_text(value):
    value = re.sub(r"\s+", " ", value or "").strip()
    return value if len(value) >= 2 else ""

def run_source(source):
    name = source["name"]
    search_url = source["base"].rstrip("/") + source["search"].replace("{{key}}", urllib.parse.quote("剑来", safe=""))
    print(f"SOURCE_START {name}: {search_url}")
    stage = "search"
    try:
        search_final, search_html = fetch(search_url)
        search_doc = BeautifulSoup(search_html, "html.parser")
        rows = select_rule(search_doc, source["search_list"])
        found = None
        for row in rows[:80]:
            title = valid_text(extract(row, source["search_name"]))
            href = extract(row, source["search_url"]).strip()
            if title and href and not href.lower().startswith(("javascript:", "#")):
                found = (title, urllib.parse.urljoin(search_final, href))
                break
        if not found:
            raise RuntimeError(f"search parsed {len(rows)} rows but found no title+URL")
        title, book_url = found
        print(f"SEARCH_OK {name}: title={title!r} url={book_url}")

        stage = "detail"
        detail_url, detail_html = fetch(book_url)
        detail_doc = BeautifulSoup(detail_html, "html.parser")
        detail_title = valid_text(extract(detail_doc, source["detail_name"]))
        if not detail_title or re.search(r"404|forbidden|验证|captcha", detail_title, re.I):
            raise RuntimeError(f"detail title rule returned invalid value {detail_title!r}")
        print(f"DETAIL_OK {name}: title={detail_title!r}")

        stage = "toc"
        toc_nodes = select_rule(detail_doc, source["toc_list"])
        chapters = []
        for node in toc_nodes[:300]:
            chapter_name = valid_text(extract(node, source["toc_name"]))
            chapter_href = extract(node, source["toc_url"]).strip()
            if chapter_name and chapter_href and not chapter_href.lower().startswith(("javascript:", "#")):
                chapters.append((chapter_name, urllib.parse.urljoin(detail_url, chapter_href)))
        if not chapters:
            raise RuntimeError(f"TOC rule returned {len(toc_nodes)} nodes but no named chapter URLs")
        chapter_name, chapter_url = chapters[0]
        print(f"TOC_OK {name}: chapters={len(chapters)} first={chapter_name!r}")

        stage = "content"
        content_url, content_html = fetch(chapter_url)
        content_doc = BeautifulSoup(content_html, "html.parser")
        content_nodes = select_rule(content_doc, source["content"])
        if not content_nodes:
            raise RuntimeError("content selector matched no node")
        content = " ".join(node.get_text(" ", strip=True) for node in content_nodes)
        content = re.sub(r"\s+", " ", content).strip()
        if len(content) < 100:
            raise RuntimeError(f"content extracted only {len(content)} characters")
        if re.search(r"访问验证|人机验证|验证码|请开启JavaScript|页面不存在|404 Not Found", content, re.I):
            raise RuntimeError(f"challenge/error page detected: {content[:160]!r}")
        print(f"CONTENT_OK {name}: chars={len(content)} chapter={chapter_name!r} url={content_url}")
        print(f"UPLOADED_CORPUS_SOURCE_PASS: {name} search -> detail -> {len(chapters)} TOC entries -> chapter content")
        return True
    except Exception as exc:
        print(f"UPLOADED_CORPUS_SOURCE_FAIL: {name}: stage={stage}: {type(exc).__name__}: {exc}")
        return False

def main():
    passed = 0
    failed = 0
    for source in SOURCES:
        if run_source(source):
            passed += 1
        else:
            failed += 1
    print(
        f"UPLOADED_CORPUS_FULL_READING: {passed}/{len(SOURCES)} source candidates "
        f"completed end-to-end; failed={failed}"
    )
    if passed == 0:
        raise SystemExit(1)

if __name__ == "__main__":
    main()
