# Runtime-3.2f.33 — BookSource six rule models and converter audit

- Branch / audit start HEAD: integration/legado-upstream-runtime / 0b8b3288569cdf1fe6b0cc5bca4f3aba884dc21b.
- Fixed upstream: LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a.
- Scope: read-only model, converter, consumer and host-adapter audit. No production/test/Gradle/workflow/UI/storage changes.

## Executive result

The pinned upstream has six distinct nested rule types, each with a custom Gson deserializer and a BookSource Room converter. The host retains six rule payloads as nullable JSON strings and has a bounded stage/selector bridge, but does not construct and execute these six typed rule models through the complete upstream semantics. Existing copied model/Gson tests provide useful shape and selected coercion coverage; they do not prove end-to-end model-to-consumer parity.

GO: keep the six raw JSON slots as a loss-preserving transport layer and retain the fail-closed bounded selector adapter as explicitly partial capability.

NO-GO: do not claim typed nested rule/converter parity or complete Legado rule execution. SelectorLegadoRuleRuntime explicitly documents itself as selector-only and does not replace upstream AnalyzeRule, stage-specific defaults, JS/regex/URL/callback semantics, or model consumers.

## Pinned upstream inventory

Source evidence at the fixed SHA:

| Type | Blob | Declared fields and defaults |
|---|---|---|
| ExploreRule | 6871515ea685dc4cb2245ecc45c952604634267d | Implements BookListRule: bookList, name, author, intro, kind, lastChapter, updateTime, bookUrl, coverUrl, wordCount; all nullable strings default null. |
| SearchRule | ba8cda4543a4afb507224ba57f0f1f5ab4ad5594 | Same ten nullable BookListRule strings plus nullable checkKeyWord (default null). |
| BookInfoRule | 54fd639003b4b4780d095c5afa379eac25a8620b | Nullable strings: init, name, author, intro, kind, lastChapter, updateTime, coverUrl, tocUrl, wordCount, canReName, downloadUrls. |
| TocRule | f525e912340257125059b6ad87d0cf7e73d6a1dc | Nullable strings: preUpdateJs, chapterList, chapterName, chapterUrl, formatJs, isVolume, isVip, isPay, updateTime, nextTocUrl. |
| ContentRule | 74020cd33c89dc0af189bae2ed68afd2c52d1446 | Nullable strings: content, subContent, title, nextContentUrl, webJs, sourceRegex, replaceRegex, imageStyle, imageDecode, payAction, callBackJs, contentBatch; nullable Int maxBatchSize = null. |
| ReviewRule | cb22a3b55e405e4303cd41987a8177000867cbc9 | Nullable strings: reviewUrl, avatarRule, contentRule, postTimeRule, reviewQuoteUrl, voteUpUrl, voteDownUrl, postReviewUrl, postQuoteUrl, deleteUrl, reviewSummaryUrl, summaryListRule, summaryParagraphIndexRule, summaryParagraphDataRule, summaryCountRule, reviewDetailUrl, reviewDetailNextPageUrl, detailListRule, detailIdRule, detailAvatarRule, detailNameRule, detailBadgeRule, detailContentRule, replyListRule, replyIdRule, replyAvatarRule, replyNameRule, replyBadgeRule, replyContentRule; enabled Boolean = false. |

Shared interface BookListRule blob: 60015b9982e10aa744c4a97b32f71cf28834eccd. It declares the ten nullable string fields used by Explore/Search; it is a contract, not a separate serialized rule object.

### Gson and Room converter semantics

- BookSource.kt blob 7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c declares @TypeConverters(BookSource.Converters::class). Each of the six rule fields has a GSON-to-string converter and a string-to-rule converter; decode uses GSON.fromJsonObject<T>(json).getOrNull(). Null/malformed persisted text therefore becomes a null rule at this converter boundary rather than a surfaced parser error.
- GsonExtensions.kt blob c0bebcb9bf072f04a680e269765bb5b2e76cd88 registers one custom deserializer per rule class. Every adapter accepts a JSON object directly, or a JSON primitive by passing json.asString to INITIAL_GSON.fromJson; JSON null/arrays/other container types return null. A primitive only succeeds as a legacy encoded rule when its string value itself is valid rule JSON; arbitrary primitive values are not guaranteed valid rule objects.
- Within rule objects, StringJsonDeserializer maps primitives via asString, JSON null to null, and objects/arrays to compact JSON text. IntJsonDeserializer accepts numeric primitives via toInt(); non-number primitives and non-primitives map to null. Missing properties retain Kotlin constructor defaults. Unknown properties are ignored by ordinary Gson model deserialization.
- Serialization is typed-model serialization, not raw JSON preservation: unknown fields are not represented in the six model classes and are discarded on a model decode/encode round trip. This differs from the host catalog's outer rawJson preservation, which does not establish typed rule behavior.

## Upstream consumer map

- Explore/Search: BookList.kt blob c542264ebe0fbaba8bbd47fb939f51cd67dd6df1 chooses getSearchRule() or getExploreRule() and consumes the shared BookListRule properties.
- Book detail: BookInfo.kt blob e8c062baaccbe7a27b8ea9b9507b6fad036b5ff1 obtains getBookInfoRule().
- TOC: BookChapterList.kt blob dd2ae3950cee55a9c8e7880dc2c034d6f4c6f33d obtains getTocRule().
- Content: BookContent.kt blob cbc353920315db9810f57aec935faeaca5310a13 consumes ContentRule, including contentBatch and maxBatchSize; other callers use content replacement, image decode and callback fields.
- Review: ReviewController.kt blob 0e5be8d8ec83094a9fdf4ae5bebf9370f64fdc7e and ReviewRuleParser.kt blob bd5f06b4125534a4cbe6f1630e80f535ef471d3f consume review summary/detail/reply properties. The upstream BookSource.getReviewRule() convenience getter is commented out; callers access nullable ruleReview directly and check enable/configuration state.

These callers demonstrate that the rule types are not interchangeable bags of selectors. Some fields are JS, URL, replacement, pagination, batch, image, callback, or metadata controls and require stage-specific execution.

## Host projection and runtime findings

- Host ReadingEngine.BookSource blob 70f1e0036a17b6d582744d612564842831b623f exposes ruleExploreJson, ruleSearchJson, ruleBookInfoJson, ruleTocJson, ruleContentJson, and ruleReviewJson as nullable strings; it does not expose the six upstream typed models.
- SourceRuleResolver.kt blob c99001865c1c4eaeae7979af7d5c85daabe9f09c selects stage JSON and extracts only string-valued fields. Non-string values are omitted from its field map.
- SelectorLegadoRuleRuntime.kt blob 829813b3f34233a6c973633b89047c6de0d6f7b5 parses string fields and routes them through configured selector modes, limited rule-expression dispatch or an injected script runtime. Its own source comment says it is a bounded selector-only runtime, not complete AnalyzeRule.
- SourceRuntime.kt blob c99001865c1c4eaeae7979af7d5c85daabe9f09c explicitly states a raw JSON rule string is not executable until a matching adapter exists. Current Search/Explore/Detail/TOC/Content services use bounded CSS parsers and optional script hooks; the review stage has no equivalent complete host service in this path.
- BadcatReadingEngineFactory.kt blob cd8946aaaffacef3d2a748a537736cf2798c96c8 wires the bounded services and Rhino host bridge; it does not convert the six rule JSON fields into the pinned rule data classes for production stage execution.

## Existing tests and coverage limits

- PinnedLegadoRuleModelsTest.kt blob 766c1cb9611deb02025cf3378bb50bd8ee10362b covers selected fields across all six models, object and legacy JSON-string forms, ReviewRule defaults/null, ContentRule batch fields and ContentRule JS string representation.
- LegadoGsonCompatibilityTest.kt blob 4e952a0679a807245fa03da2bdb76c81eb6febb8 covers object/string forms for all six, null/array adapter behavior, unknown-field discard, selected scalar coercion and serialization round trips. Despite its method name, allSixRuleConvertersAcceptObjectAndStringEncodedObject calls GSON directly; it does not instantiate the Room BookSource.Converters methods.
- ContentRuleCompatibilityTest.kt blob 31c8d455bdb9a0e7f6736ec329428a9c85187433 does exercise actual BookSource.Converters round trips, but only for ContentRule and its legacy JS toString() contract.
- No evidence in this audit establishes per-property consumer equivalence for all six models, full Room converter behavior for all six, or complete upstream rule execution.

## Minimal follow-up sequence

1. Add focused converter tests that instantiate the six actual converter methods, including null, malformed JSON, object, legacy JSON-string primitive, unknown fields and serialization defaults. Keep database column defaults separate from Kotlin/Gson constructor defaults.
2. Add field-by-field fixtures for all six models, including ReviewRule's currently dormant/partial operations and ContentRule batch semantics.
3. Register each runtime stage separately for typed model binding and caller behavior. Do not route every string-valued field through CSS; preserve URL, JS, regex/replacement, callback, batch and pagination semantics.
4. Treat ReviewRule as its own later runtime slice with explicit enable/configuration and summary/detail/reply flows.
5. Do not broaden Gson coercion globally without fixed-upstream differential fixtures.

## Decision

- GO: retain the current raw JSON DTO and bounded selector subset as partial compatibility layers.
- NO-GO: typed rule model/converter parity, full AnalyzeRule behavior, ReviewRule integration and complete BookSource runtime acceptance remain unproven/incomplete.
- This is a read-only audit. No build, CI, APK, network-source, emulator or physical-device evidence is claimed.