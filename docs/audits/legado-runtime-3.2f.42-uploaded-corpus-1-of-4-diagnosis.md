# Runtime-3.2f.42 — uploaded-corpus 1/4 live probe diagnosis

Date: 2026-11-23
Branch: integration/legado-upstream-runtime
Pinned Legado SHA: 330e606c4d57e08adc056ff8ab7a4132c784da9a

## Scope

Read-only diagnosis of the four-source live probe result. Registration was committed before investigation at 56fb2b2237ec27689377ac37dded76452bb7f9e1. No production, test, source fixture, workflow, UI, storage or build files were changed. Only this report and the third-category checklist are changed.

## Exact CI evidence

| Run | Run ID | Exact SHA | CI conclusion | Corpus artifact |
|---:|---:|---|---|---:|
| #3221 | 45193908754 | 88099116db0a86365ae162a67b1639d9e1415b6f | success | 14827636953 |
| #3222 | 45193966686 | 2aa0b6524bccdf74fcf95f52691a7fd86fd4122b | success | 14829136170 |
| #3223 | 45195161068 | bf4fa7f85be3afa2ff1eb5dda7d5e098f91b3c01 | success | 14830136420 |
| #3224 | 45195170807 | a45faf2104b220fe6712c9ab10f952abc2e76b3d | success | 14829062604 |
| #3225 | 45196110187 | 06b9284379c6fbaa559724e5df23216d3bd1c349 | success | 14827498487 |

The #3223, #3224 and #3225 corpus logs were downloaded and inspected. Their jobs concluded successfully. The uploaded corpus probe is marked best-effort / continue-on-error in the workflow; a failed source probe does not fail the overall Actions run.

## Key test-validity finding

The reported 1/4 is NOT an Android Badcat / pinned Legado runtime end-to-end result.

The workflow step at .github/workflows/android-build.yml lines 1019–1033 invokes .github/scripts/live-uploaded-source-reading-smoke.py directly with CPython after installing BeautifulSoup. The script hardcodes four descriptors in its SOURCES array; it does not load the uploaded corpus file at runtime. It uses Python urllib.request and BeautifulSoup, not the app Kotlin HTTP gateway, Rhino, AnalyzeRule/AnalyzeUrl, JsExtensions, Cookie/login/session or source lifecycle. It supplies one generic fixed Android Chrome-like User-Agent rather than each source's own request header/JS/login configuration. It implements only a small custom subset of CSS selector behavior and no Legado JS/rule semantics. It uses a fixed 25-second timeout, three attempts and its own retry policy.

Therefore 1/4 measures this CI runner's ability to reach and scrape four hardcoded URLs with a simplified Python probe. It cannot establish that the Badcat client supports only one of four sources, and it cannot certify the three failing sources against the actual app runtime. This is a test-harness validity/coverage gap.

## Per-source observations

### 唐三中文 — repeated search-stage network timeout

URL: http://www.xtangsanshu.com/s.php?ie=utf-8&s=&q=%E5%89%91%E6%9D%A5

Across #3223, #3224 and #3225 the probe records three attempts per run, each around 25 seconds, all URLError timed out. Failure occurs at search before rule parsing. This is consistent with endpoint/runner reachability or HTTP connection/read timeout; logs do not separate DNS, TCP connect, server wait or proxy causes. This is not evidence of a Badcat rule-engine defect.

### 零零小说 — repeated search-stage network timeout

URL: https://www.00shu.la/modules/article/search.php?q=%E5%89%91%E6%9D%A5

Across #3223, #3224 and #3225 the probe records three attempts per run, each around 26 seconds, all URLError timed out. Failure occurs before HTML parsing. The artifact does not reveal whether the timeout is DNS, TCP/TLS establishment, response headers or body read. This is not evidence of a Badcat rule-engine defect.

### 笔趣阁fun — unstable search endpoint result

URL template: https://www.bqg.fun/search.php?searchkey= (query encoded as %E5%89%91%E6%9D%A5)

Results vary by run: #3223 has two read timeouts around 25 seconds, then HTTP 404 on attempt 3; #3224 has three read timeouts around 25 seconds; #3225 returns HTTP 404 on the first attempt after 19.69 seconds. The probe does not preserve response headers/body for HTTPError, so we cannot tell whether 404 is a missing route, CDN/WAF/interstitial response, bot/region policy, or reverse-proxy response. A single 404 must not be treated as a stable source-rule diagnosis. This is undetermined endpoint/site behavior, not a proven client runtime bug.

### 精华书阁 — successful control

URL: https://m.jhsssd.com/search.html?ie=utf-8&word=%E5%89%91%E6%9D%A5&page=1

Across #3223–#3225, search, detail, TOC and chapter content all pass. Responses are fast (typically below 0.5 seconds), status 200. This validates the Python probe path for this one site only.

## Additional real-source probes

In #3223 and #3225, separate Bixiange and Pinshuzhai probes passed search → detail → TOC → content using dedicated scripts. Pinshuzhai had one transient 50.62-second read timeout, then succeeded on retry; #3225 completed detail/TOC/content. This demonstrates intermittent live host behavior, but those scripts also remain Python smoke probes and are not proof of Android client runtime parity.

## Root-cause classification

- 唐三中文: undetermined network/site reachability; repeated timeout before parsing.
- 零零小说: undetermined network/site reachability; repeated timeout before parsing.
- 笔趣阁fun: undetermined site/endpoint response; intermittent timeouts and 404; response metadata/body missing.
- 精华书阁: Python probe full-flow success.
- Harness-wide: confirmed probe does not exercise the Badcat Legado runtime and does not actually load the uploaded source corpus. The aggregate 1/4 is not a valid client compatibility score.

## Recommended next work (requires separate preregistration)

1. Replace/augment the Python probe with a real app/instrumentation test invoking the pinned Legado runtime and imported source objects, preserving source-specific headers, JS, Cookie and login semantics.
2. Preserve sanitized HTTP diagnostics: DNS/connect/TLS/TTFB/body timings where available, redirect chain, final URL, status, allowlisted response headers, body byte count and bounded redacted body fingerprint/snippet.
3. Re-run the three failing hosts from at least two independent runner/network locations before assigning blame; record exact source descriptor hash and run SHA.
4. For bqg.fun, capture 404 headers/body and compare current source endpoint with a known-good browser/manual request before editing the source rule.
5. Keep timeout and retry values bounded; do not mask failures by increasing limits or skipping stages.

## Limits

Existing logs cannot identify the underlying cause of the two timeouts or bqg.fun 404. No response body/header, DNS/connect/TLS phase breakdown, browser control, or independent runner comparison was captured. No source rule was edited. No local test/build, APK install or device validation was performed.

## Status

Runtime-3.2f.42 read-only diagnosis is closed with the harness validity gap confirmed and per-source causes classified as above. A production fix or new live-probe implementation must be separately preregistered. Third category remains partially implemented; full Legado compatibility is not established.