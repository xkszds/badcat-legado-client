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
