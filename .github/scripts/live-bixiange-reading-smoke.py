#!/usr/bin/env python3
"""Live end-to-end smoke test for the public Bixiange Legado source.

Validates the imported source's GBK POST search, detail page, java.ajax-style
TOC first-page request, and non-empty chapter content. HTTP 200 alone is not
counted as a successful reading flow.
"""
import json
import re
import urllib.parse
import urllib.request

SOURCE_URL = "https://raw.githubusercontent.com/speauty/legado.sources/main/sources/active/bixiange.json"
KEYWORDS = ("斗罗大陆", "斗破苍穹", "完美世界")
MAX_BYTES = 2 * 1024 * 1024
TIMEOUT = 12


def fetch(url, headers, method="GET", body=None, charset="utf-8"):
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
        raw = response.read(MAX_BYTES + 1)
        if len(raw) > MAX_BYTES:
            raise RuntimeError(f"response exceeded {MAX_BYTES} bytes: {url}")
        final_url = response.geturl()
        status = response.status
        content_type = response.headers.get("Content-Type", "")
    if status < 200 or status >= 300:
        raise RuntimeError(f"HTTP {status}: {url}")
    declared = re.search(r"charset=([\w-]+)", content_type, re.I)
    encodings = [declared.group(1)] if declared else []
    encodings += [charset, "utf-8", "gbk"]
    text = None
    for encoding in dict.fromkeys(encodings):
        try:
            text = raw.decode(encoding)
            break
        except (LookupError, UnicodeDecodeError):
            continue
    if text is None:
        raise RuntimeError(f"could not decode response: {url}; content-type={content_type}")
    return final_url, text


def main():
    _, source_text = fetch(SOURCE_URL, {"User-Agent": "Mozilla/5.0"})
    source_data = json.loads(source_text.lstrip("\ufeff"))
    if not isinstance(source_data, list) or not source_data:
        raise RuntimeError("Bixiange source descriptor is not a non-empty JSON array")
    source = source_data[0]
    if source.get("bookSourceName") != "笔仙阁" or source.get("enabled") is False:
        raise RuntimeError("Expected enabled 笔仙阁 source descriptor")

    base = source["bookSourceUrl"].rstrip("/") + "/"
    headers = dict(source.get("header") or {})
    headers.setdefault("User-Agent", "Mobile Safari/537.36")
    headers.setdefault("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    headers.setdefault("Referer", base)

    descriptor = source["searchUrl"]
    separator = re.search(r",\s*(?=\{)", descriptor)
    if not separator:
        raise RuntimeError("Bixiange searchUrl has no static request options")
    search_endpoint = descriptor[:separator.start()].strip()
    options = json.loads(descriptor[separator.start() + 1:])
    charset = options.get("charset", "utf-8")
    method = options.get("method", "GET").upper()
    body_template = options.get("body", "")
    candidate = None
    keyword_used = None
    from bs4 import BeautifulSoup

    for keyword in KEYWORDS:
        encoded_keyword = urllib.parse.quote_from_bytes(keyword.encode(charset), safe="")
        body_text = body_template.replace("{{key}}", encoded_keyword)
        body_bytes = body_text.encode("ascii") if method == "POST" else None
        request_headers = dict(headers)
        if method == "POST":
            request_headers.setdefault("Content-Type", f"application/x-www-form-urlencoded; charset={charset}")
        search_url = urllib.parse.urljoin(base, search_endpoint)
        _, search_html = fetch(search_url, request_headers, method, body_bytes, charset)
        soup = BeautifulSoup(search_html, "html.parser")
        rows = soup.select(".list ul li")
        print(f"LIVE_BIXIANGE_SEARCH_ATTEMPT: keyword={keyword!r}, rows={len(rows)}, status=200")
        for row in rows:
            anchor = row.select_one(".title a")
            if anchor and anchor.get("href") and anchor.get_text(" ", strip=True):
                candidate = (anchor.get_text(" ", strip=True), urllib.parse.urljoin(base, anchor["href"]))
                break
        if candidate:
            keyword_used = keyword
            break
    if not candidate:
        raise RuntimeError("Bixiange search returned no usable .list ul li > .title a entries for tested keywords")

    book_name, detail_url = candidate
    print(f"LIVE_BIXIANGE_SEARCH_OK: keyword={keyword_used!r}, name={book_name!r}, url={detail_url}")

    final_detail_url, detail_html = fetch(detail_url, headers)
    detail_soup = BeautifulSoup(detail_html, "html.parser")
    detail_title = detail_soup.select_one(".desc h1")
    if detail_title is None or not detail_title.get_text(" ", strip=True):
        raise RuntimeError(f"detail page has no .desc h1 title: {final_detail_url}")
    print(f"LIVE_BIXIANGE_DETAIL_OK: title={detail_title.get_text(' ', strip=True)!r}")

    # Mirrors the imported rule's java.ajax(root + '/index/1.html') call.
    root = final_detail_url.rstrip("/")
    toc_first_url = root + "/index/1.html"
    final_toc_url, toc_html = fetch(toc_first_url, headers)
    total_match = re.search(r"(?:^|[^0-9])1/(\d+)(?:<|[^0-9])", toc_html)
    total_pages = int(total_match.group(1)) if total_match else 0
    if total_pages < 1:
        raise RuntimeError(f"TOC first page did not expose the source's 1/N pagination marker: {final_toc_url}")
    print(f"LIVE_BIXIANGE_TOC_OK: total_pages={total_pages}, first_page={final_toc_url}")

    content_soup = BeautifulSoup(toc_html, "html.parser")
    paragraphs = content_soup.select("#mycontent p")
    content = "\n".join(node.get_text(" ", strip=True) for node in paragraphs).strip()
    if not content:
        raise RuntimeError(f"chapter content rule #mycontent p returned empty text: {final_toc_url}")
    print(f"LIVE_BIXIANGE_CONTENT_OK: chars={len(content)}, preview={content[:100]!r}")
    print("LIVE_BIXIANGE_PASS: GBK POST search -> detail -> TOC ajax page -> chapter content")


if __name__ == "__main__":
    main()
