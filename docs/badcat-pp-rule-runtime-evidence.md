# Badcat PPRule runtime evidence

This note records read-only observations from the original Badcat 1.5.0.5 companion DEX ZIP. It separates confirmed consumers from assumptions; it is not a claim that the new client has implemented this legacy runtime.

## Confirmed model

`sens.Pp.PPRule` is a protobuf message with these fields:

- `ruleType` (enum)
- `pageRule` (string)
- `nextPageRule` (string)
- `pageJs` (string)
- `chapterIsReverse` (boolean)
- `videoPathRule` (string)
- `videoEpisodeRule` (string)
- `pageJsForSingleHtml` (string)

The generated enum exposes `RULE_TYPE_RE`, `RULE_TYPE_XPATH` and `UNRECOGNIZED`. The `PPConfigItem` message groups separate rules for author, book name, content and list, and also has a separate `pc2WapRule` string and data type. The enclosing response has version and rule-version fields.

## Confirmed runtime consumers

The original `com.read.translib.TransEngine` is the dispatch layer. DEX method references show:

- It receives a `PPResponse`, checks response status/version, stores a backup, and builds per-host rule maps.
- It branches by `Base.DataType` (novel, comic, video) and checks the corresponding enable flag on `TransManager`.
- `transBookAuthor` and `transBookName` fetch the corresponding `PPRule` from `PPConfigItem` and call the legacy rule evaluator.
- `transList` and `transComicList` use the list rule. `isListReverse` reads `PPRule.chapterIsReverse`.
- `transContent` uses the content rule.
- `transComicContent` checks `pageJsForSingleHtml`; when present it invokes the comic single-HTML JavaScript path, otherwise it enters the comic page parser. The comic parser reads `pageRule`, and the comic utility also reads `pageJs`.
- `transVideoList` dispatches to the video utility, which uses `videoPathRule` and `videoEpisodeRule` to extract path and episode elements. The video utility reads episode links from `href`, episode labels from element text, and resolves relative URLs.

The obfuscated helper `a/b/a/a/f` has an observed `RULE_TYPE_RE` path that compiles `pageRule`, calls `Matcher.find()`, and reads capture group 1. Its non-regex path parses HTML with JSoup and selects the first matching element; a `meta` result reads its `content` attribute, otherwise it reads element text. Other call sites use the same helper for author/title/content/list extraction.

The comic page utility reads image `data-original` first and falls back to `src` when empty. When its JavaScript path is used, the result is parsed as a JSON array of image URLs.

## Important boundaries

- PPRule is a page/source extraction and pagination/comic/video rule family. It is not the same thing as Legado's chapter-body replacement rules and must not be inserted into the existing “正文替换” feature.
- `nextPageRule` is present in the protobuf schema, but the scan found no non-generated DEX consumer reference to its getter. Its runtime semantics remain unconfirmed.
- The exact protobuf field numbers and every branch/edge case of the obfuscated helper methods still need a second pass before implementing a wire-compatible decoder.
- The new client has not yet implemented this legacy PPRule runtime. Keep the original fields opaque until their full wire and execution contracts are established.


## Protobuf wire schema recovered from DEX constants and the bundled sample (2026-11-17)

The original APK's generated sens.Pp classes expose the exact *_FIELD_NUMBER constants. These field numbers were also cross-checked against assets/trans.proto (802,268 bytes) from the same APK; the file decodes as a PPResponse with status/message, version and repeated host configuration records.

| Message | Field number | Field | Wire/value type |
|---|---:|---|---|
| PPResponse | 1 | code | varint |
|  | 2 | message | UTF-8 string |
|  | 3 | version | varint |
|  | 4 | ppItems | repeated PPConfigItem |
|  | 5 | scriptCode | UTF-8 string |
|  | 6 | scriptSignature | UTF-8 string |
|  | 7 | ppRuleVer | varint |
|  | 8 | bookDetailScript | UTF-8 string |
|  | 9 | jsLibScript | UTF-8 string |
| PPConfigItem | 1 | host | UTF-8 string |
|  | 2 | listRule | PPRule |
|  | 3 | contentRule | PPRule |
|  | 4 | wap2PcRule | UTF-8 string |
|  | 5 | pc2WapRule | UTF-8 string |
|  | 6 | dataType | varint enum value |
|  | 7 | bookNameRule | PPRule |
|  | 8 | authorRule | PPRule |
|  | 9 | isNotUseReferer | varint boolean |
| PPRule | 1 | ruleType | varint enum (RE=0, XPATH=1) |
|  | 2 | pageRule | UTF-8 string |
|  | 3 | nextPageRule | UTF-8 string |
|  | 4 | pageJs | UTF-8 string |
|  | 5 | chapterIsReverse | varint boolean |
|  | 6 | videoPathRule | UTF-8 string |
|  | 7 | videoEpisodeRule | UTF-8 string |
|  | 8 | pageJsForSingleHtml | UTF-8 string |

The bundled sample contains 1,583 repeated PPConfigItem records. It uses XPath-valued PPRule records and also contains RE rules where the enum field is omitted (default zero). This confirms that field 1 is the rule-type enum and field 2 is the rule expression; it is not safe to treat every pageRule string as CSS.

## Implemented bounded PPRule slice

- Added BadcatPpRuleProtoDecoder: bounded read-only decoding of PPResponse, PPConfigItem and all eight PPRule fields. It preserves unknown wire fields as raw slices, caps message/string/item/field sizes, rejects malformed varints and invalid UTF-8, and does not execute server-supplied scripts.
- Added BadcatPpRuleEvaluator: RE scalar extraction uses the bounded regex engine and returns capture group 1, matching the recovered helper path. XPath scalar extraction selects the first node; meta reads its content attribute and other elements return text. Collection selection returns regex matches or XPath-selected fragments.
- Contract tests cover all known field numbers, unknown-field preservation, malformed wire data, RE group-1 extraction, XPath meta/text extraction and collection selection.

The bounded evaluator also exposes a chapter-list ordering helper that applies the verified `chapterIsReverse` flag only when the caller explicitly identifies a novel chapter list; it is not applied implicitly to comic pages or video episodes.

The bounded evaluator now also exposes a two-stage video adapter for the separately recovered `videoPathRule` and `videoEpisodeRule` fields: it selects path containers first, applies the episode selector within each container, then resolves/deduplicates HTTP(S) links. This is a selector-based compatibility slice; original player grouping, ordering, pagination, and UI semantics are still not claimed.

The bounded evaluator now also exposes two evidence-backed helpers: video link extraction resolves HTTP(S) hrefs and uses title/aria-label/text label fallbacks; comic image extraction prefers data-original, falls back to src when empty, resolves relative URLs, rejects unsafe schemes/credential URLs, and de-duplicates normalized URLs. Contract tests cover those helpers. These helpers do not yet recreate the original utility's full grouping, ordering, pagination, or UI behavior.

Still not implemented: pageJs, pageJsForSingleHtml execution, nextPageRule pagination behavior, full comic page ordering/loading pipeline, video path/episode joining, and the complete TransEngine novel/comic/video dispatch. dataType remains a raw numeric value until every enum mapping and dispatch edge is confirmed. The new decoder and extraction helpers are a compatibility foundation, not a claim that the complete legacy PPRule runtime is finished.
