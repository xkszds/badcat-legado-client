#!/usr/bin/env python3
"""Live full-reading smoke for a representative source from the uploaded 235-source corpus.

The source descriptor is copied from the user-provided corpus without its private
headers or session material. This checks real search -> detail -> TOC -> content,
not merely HTTP status or a non-empty response.
"""
import re
import sys
import urllib.parse
import urllib.request

from bs4 import BeautifulSoup

BASE = "http://www.xtangsanshu.com"
SEARCH = "/s.php?ie=utf-8&s=&q={{key}}"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
}
MAX_BYTES = 3 * 1024 * 1024
KEYWORDS = ("斗破苍穹", "凡人修仙传", "道君")


def fetch(url):
    request = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(request, timeout=22) as response:
        body = response.read(MAX_BYTES + 1)
        if len(body) > MAX_BYTES:
            raise RuntimeError(f"response exceeds {MAX_BYTES} bytes")
        if response.status < 200 or response.status >= 300:
            raise RuntimeError(f"HTTP {response.status}")
        charset = response.headers.get_content_charset() or "utf-8"
        try:
            text = body.decode(charset)
        except (LookupError, UnicodeDecodeError):
            text = body.decode("utf-8", errors="replace")
        return response.geturl(), text


def clean_text(node):
    return re.sub(r"\\s+", " ", node.get_text(" ", strip=True)).strip() if node else ""


def main():
    selected = None
    for keyword in KEYWORDS:
        url = BASE + SEARCH.replace("{{key}}", urllib.parse.quote(keyword, safe=""))
        final_url, html = fetch(url)
        soup = BeautifulSoup(html, "html.parser")
        rows = soup.select(".bookbox")
        print(f"SEARCH_ATTEMPT keyword={keyword!r} rows={len(rows)} final_url={final_url}")
        for row in rows:
            name_node = row.select_one(".bookname")
            link = row.select_one("a[href]")
            title = clean_text(name_node)
            href = link.get("href", "") if link else ""
            if title and href and not href.lower().startswith("javascript:"):
                book_url = urllib.parse.urljoin(final_url, href)
                selected = (keyword, title, book_url)
                break
        if selected:
            break
    if not selected:
        raise RuntimeError("search: no row with both a real title and book URL")

    keyword, search_title, book_url = selected
    print(f"SEARCH_OK title={search_title!r} book_url={book_url}")

    detail_url, detail_html = fetch(book_url)
    detail = BeautifulSoup(detail_html, "html.parser")
    detail_title = clean_text(detail.select_one(".info h2"))
    if not detail_title:
        raise RuntimeError("detail: source rule .info@h2@text produced no title")
    if len(detail_title) < 2 or any(x in detail_title.lower() for x in ("404", "forbidden", "验证")):
        raise RuntimeError(f"detail: invalid title {detail_title!r}")
    print(f"DETAIL_OK title={detail_title!r} url={detail_url}")

    # Source ruleToc.chapterList is .listmain@dd@a; the book detail page itself
    # carries this chapter list (ruleBookInfo does not declare a separate tocUrl).
    chapter_nodes = detail.select(".listmain dd a")
    chapters = []
    for node in chapter_nodes:
        chapter_name = clean_text(node)
        href = node.get("href", "")
        if chapter_name and href and not href.lower().startswith("javascript:"):
            chapters.append((chapter_name, urllib.parse.urljoin(detail_url, href)))
    if not chapters:
        raise RuntimeError("toc: source rule .listmain@dd@a returned no named chapter URLs")
    chapter_name, chapter_url = chapters[0]
    print(f"TOC_OK chapters={len(chapters)} first={chapter_name!r} url={chapter_url}")

    content_url, content_html = fetch(chapter_url)
    content_doc = BeautifulSoup(content_html, "html.parser")
    content_node = content_doc.select_one("#content")
    if content_node is None:
        raise RuntimeError("content: source rule id.content@html did not match #content")
    # Remove script/style and known navigation noise before validating real chapter text.
    for node in content_node.select("script,style,noscript"):
        node.decompose()
    content = clean_text(content_node)
    if len(content) < 100:
        raise RuntimeError(f"content: extracted only {len(content)} characters")
    if re.search(r"访问验证|人机验证|验证码|请开启JavaScript|页面不存在|404 Not Found", content, re.I):
        raise RuntimeError(f"content: challenge/error page detected: {content[:180]!r}")
    print(f"CONTENT_OK chars={len(content)} chapter={chapter_name!r} url={content_url}")
    print(f"UPLOADED_CORPUS_SOURCE_PASS: 唐三中文; search -> detail -> {len(chapters)} TOC rows -> chapter body")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"UPLOADED_CORPUS_SOURCE_FAIL: {type(exc).__name__}: {exc}")
        raise
