#!/usr/bin/env python3
"""Bounded end-to-end live smoke test for a static Legado novel source.

Checks real search -> detail -> TOC -> chapter body and reports the stage
that fails. This is an external-site compatibility probe, not a fixture test.
"""
import gzip
import json
import re
import urllib.parse
import urllib.request

from bs4 import BeautifulSoup

COLLECTION_URL = "https://shuyuan.nyasama.cc/shuyuan/12f348d3510bac0e5f1b164274f0b136.json"
KEYWORDS = ("斗破苍穹", "剑来", "凡人修仙传")
MAX_BYTES = 2 * 1024 * 1024
TIMEOUT = 10


def fetch_text(url, headers):
    request = urllib.request.Request(url, headers={**headers, "Accept-Encoding": "identity"})
    with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
        body = response.read(MAX_BYTES + 1)
        if len(body) > MAX_BYTES:
            raise RuntimeError(f"response exceeded 2 MiB: {url}")
        if not 200 <= response.status < 300:
            raise RuntimeError(f"HTTP {response.status}: {url}")
        charset = response.headers.get_content_charset() or "utf-8"
        return response.geturl(), body.decode(charset, errors="replace")


def main():
    request = urllib.request.Request(
        COLLECTION_URL,
        headers={"Accept-Encoding": "identity", "User-Agent": "Mozilla/5.0"},
    )
    with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
        raw = response.read(8 * 1024 * 1024 + 1)
    if len(raw) > 8 * 1024 * 1024:
        raise RuntimeError("source collection exceeded 8 MiB")
    if raw[:2] == bytes((31, 139)):
        raw = gzip.decompress(raw)
    sources = json.loads(raw.decode("utf-8-sig"))
    source = next(
        (item for item in sources
         if item.get("bookSourceName") == "飞速中文" and item.get("enabled") is not False),
        None,
    )
    if source is None:
        raise RuntimeError("enabled 飞速中文 source was not found")

    headers = json.loads(source.get("header") or "{}")
    if not isinstance(headers, dict):
        headers = {}
    headers.setdefault("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
    base = source["bookSourceUrl"]
    candidate = None
    used_keyword = None
    search_final_url = None
    for keyword in KEYWORDS:
        search_url = source["searchUrl"].replace("{{key}}", urllib.parse.quote(keyword, safe=""))
        search_final_url, html = fetch_text(search_url, headers)
        soup = BeautifulSoup(html, "html.parser")
        rows = soup.select(".nlist")
        for row in rows:
            link = row.select_one("h3 a")
            if link and link.get_text(" ", strip=True) and link.get("href"):
                candidate = (link.get_text(" ", strip=True), link["href"])
                used_keyword = keyword
                break
        print(f"LIVE_E2E_SEARCH_ATTEMPT: keyword={keyword!r}, final_url={search_final_url!r}, list_nodes={len(rows)}, candidate={candidate!r}")
        if candidate:
            break
    if not candidate:
        raise RuntimeError(f"search: no .nlist h3 a result for {KEYWORDS!r}; final_url={search_final_url!r}")

    title, raw_book_url = candidate
    match = re.search(r"(\\d+)", raw_book_url)
    if not match:
        raise RuntimeError(f"search: book link has no numeric book id: {raw_book_url!r}")
    book_url = urllib.parse.urljoin(base, f"/Html/{match.group(1)}/index.html")
    print(f"LIVE_E2E_SEARCH_OK: keyword={used_keyword!r}, title={title!r}, book_url={book_url}")

    detail_final_url, detail_html = fetch_text(book_url, headers)
    detail = BeautifulSoup(detail_html, "html.parser")
    og_title = detail.select_one('meta[property="og:title"]')
    detail_name = (og_title.get("content", "").strip() if og_title else "") or (detail.select_one("h1").get_text(" ", strip=True) if detail.select_one("h1") else "")
    if not detail_name:
        raise RuntimeError(f"detail: no og:title or h1; final_url={detail_final_url!r}; title={detail.title.get_text(' ', strip=True) if detail.title else ''!r}")
    print(f"LIVE_E2E_DETAIL_OK: title={detail_name!r}, final_url={detail_final_url!r}")

    toc_rows = detail.select(".chapterlist ul li a")
    chapter_link = next((link for link in toc_rows if link.get("href") and link.get_text(" ", strip=True)), None)
    if chapter_link is None:
        raise RuntimeError(f"TOC: no .chapterlist ul li a links; matched={len(toc_rows)}; final_url={detail_final_url!r}")
    chapter_title = chapter_link.get_text(" ", strip=True)
    chapter_url = urllib.parse.urljoin(detail_final_url, chapter_link["href"])
    print(f"LIVE_E2E_TOC_OK: chapters={len(toc_rows)}, first_chapter={chapter_title!r}, url={chapter_url}")

    content_final_url, content_html = fetch_text(chapter_url, headers)
    content_doc = BeautifulSoup(content_html, "html.parser")
    paragraphs = content_doc.select("#content > p")
    content = "\\n".join(p.get_text(" ", strip=True) for p in paragraphs if p.get_text(" ", strip=True))
    if not content:
        root = content_doc.select_one("#content")
        content = root.get_text("\\n", strip=True) if root else ""
    if not content:
        raise RuntimeError(f"content: #content>p yielded no text; final_url={content_final_url!r}; bytes={len(content_html)}")
    print(f"LIVE_E2E_CONTENT_OK: chapter={chapter_title!r}, chars={len(content)}, preview={content[:100]!r}")
    print("LIVE_E2E_PASS: search -> detail -> TOC -> content all returned usable data")


if __name__ == "__main__":
    main()
