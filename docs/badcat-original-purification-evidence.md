# Badcat 1.5.0.5 original purification: DEX evidence and integration boundary

## Reference inputs

- Original APK: 坏坏猫_1.5.0.5.apk
- Companion DEX archive: com.hhm.verticalsearch.zip
- The APK and DEX archive are read-only reference inputs. Do not patch, resign, overwrite, or bypass native/signature checks.

## Directly observed DEX symbols and strings

A raw string-table scan of the companion DEX archive found the following symbols.

### Web/ad-blocking path

- ADBlockUtils
- AdBlockPresenter
- AdMarkDao
- AdMarkHttpApi
- AdMarkService
- SettingAdBlockActivity
- handleAdBlock, handleAdMark, handleInjectAdBlock
- getAdBlockJsInject, key_ad_block_js_inject, key_is_ad_block, mIsAdBlock, splashAdBlock
- Endpoint path strings: adMark/issued and adMark/report

These are static symbol/string observations. They suggest a web ad-filtering path involving injected JavaScript and a separate ad-mark reporting/rule service. They do not reveal the full request schema, response semantics, injection timing, or DOM behavior.

### sens.Pp.PPRule rule container

The DEX string table contains protobuf field-name constants for:

- chapterIsReverse
- nextPageRule
- pageJs
- pageJsForSingleHtml
- pageRule
- ruleType
- videoEpisodeRule
- videoPathRule

Related strings include transcode/ppRule and ppRuleVer.

These names indicate page/chapter pagination and video/episode rule data. They must not be treated as ordinary novel-body replacement rules without method-level evidence.

### sens.AdMark protobuf family

The DEX contains generated message names including:

- AdMarkIssuedRequest, AdMarkIssuedResponse
- AdMarkReportRequest, AdMarkReportResponse
- AdMarkItem, AdMarkRuleItem

Field-name strings include markItems, markRules, isFilter, isBlock, host, and urlPattern in the broader generated protobuf pool. The exact owning message for each field still needs to be confirmed from generated descriptors or decompiled methods.

## Integration boundary

Keep these rule domains independent:

| Domain | Input | Required adapter |
|---|---|---|
| Legado text replacement | Chapter title/body text | CatalogLegadoBookTextReplacement |
| Badcat WebView ad filtering | WebView URL/DOM/script | A separately verified JS-injection adapter |
| Badcat ad marks | URL/host/mark metadata | A separately verified AdMark repository and matcher |
| Badcat page/catalog/video rules | Page HTML, chapter lists, video metadata | A separately verified PPRule adapter |

Do not merge PPRule or AdMarkRuleItem into Legado ReplaceRule JSON. Do not silently disable the original ad-block toggle when importing Legado text rules.

## Current implementation status

- Legado rule JSON import/export and local persistence: implemented.
- Legado title/body scope selection: implemented.
- Literal replacement: implemented.
- Bounded Java/Kotlin regex replacement: implemented with timeout detection; timed-out rules are disabled in the local catalog.
- JavaScript @js replacement expressions: bounded Rhino execution is wired with result/book/chapter context and per-rule helper state (log, logType, get, put, MD5, digest and HMAC). This is a limited host, not full upstream RegexJsExtensions/JsEncodeUtils compatibility; Chinese conversion and symmetric/asymmetric crypto helpers are not yet ported.
- Development-only rule manager screen: implemented; it is not the original Badcat settings UI.
- Original Badcat WebView JS injection, AdMark service, and PPRule execution: not ported.
- Original Badcat UI/resource reconstruction: not yet integrated into the host app.

## Next verification tasks

1. Obtain method-level disassembly/decompilation for ADBlockUtils, AdBlockPresenter, AdMarkService, AdMarkHttpApi, and SettingAdBlockActivity.
2. Trace where getAdBlockJsInject, handleInjectAdBlock, handleAdMark, and transcode/ppRule are called.
3. Decode the protobuf descriptors for PPRule and AdMarkRuleItem to establish field ownership, types, and defaults.
4. Identify whether adMark/issued and adMark/report use public service contracts or app-private authenticated APIs. Do not copy private credentials or bypass access controls.
5. Implement each verified domain behind a separate interface and test against captured, user-authorized fixtures.
6. Rebuild the original purification settings/reader affordances only after the relevant layouts and event handlers are verified.

## Safety and accuracy

This document records string-table evidence only. It does not claim the original purification algorithm has been recovered, nor that the current Legado text replacement engine reproduces Badcat's ad filtering.


## Method-level DEX verification (2026-11-17)

The companion DEX archive was materialized read-only and the standard DEX method tables/code items were inspected for the relevant classes. This adds method-level evidence beyond the earlier string-table inventory; it is still not a complete source decompilation.

### Confirmed WebView ad-block flow

- `SettingAdBlockActivity.initView()` reads the persisted Boolean preference `key_is_ad_block` and applies it to the ad-block toggle.
- The setting screen is titled “广告过滤”. Its empty-state text says “没有已标识的广告 / 可在网页中长按进行标记”, confirming that the list is a user-managed set of marked ads, not a generic text-replacement list.
- `CustomWebChromeClient.onPageFinish()` calls `handleAdBlock()`.
- `CustomWebChromeClient.handleAdBlock()` calls `WebViewInjectHelper.initWebViewGetAdUrlPatternList()`; the callback path ultimately passes the retrieved URL-pattern list into the helper that removes ad elements from the WebView.
- `CustomWebChromeClient.onReceivedTitle()` conditionally calls `handleInjectAdBlock()`. That method reads `key_ad_block_js_inject` and, when non-empty, passes the stored JavaScript to `loadJS()`, which uses WebView JavaScript evaluation / URL loading.
- `WebViewInjectHelper.initWebViewGetAdUrlPatternList()` first checks the local ad-pattern repository. If it has no patterns, it calls the WebView JavaScript bridge handler `getInfoFromJS` and persists the returned pattern list.
- `WebViewInjectHelper.handleAdMark()` deserializes a `JsAdMarkInfo`, asks the user to confirm “确定标记该广告？”, then stores the mark and invokes the WebView callback that removes the selected ad.

### Confirmed AdMark data flow

- The issued response carries a JavaScript injection string (`adBlockJsInject`), a version, and `markItems`.
- Each `AdMarkItem` has a `host` and a list of `AdMarkRuleItem` records.
- Each `AdMarkRuleItem` has a `urlPattern` and repeated `markRules` string values. The host code converts these into local `DbAdMark` records; matching uses the URL pattern and the mark-rule data to identify a URL position/pattern.
- A separate report response path exists. This confirms that original marked-ad handling has local persistence plus service synchronization; it must not be silently replaced by Legado text rules.

### Confirmed PPRule schema boundary

The generated protobuf message `sens.Pp.PPRule` has typed fields for:

- `ruleType` (integer)
- `pageRule`
- `nextPageRule`
- `pageJs`
- `pageJsForSingleHtml`
- `chapterIsReverse` (Boolean)
- `videoPathRule`
- `videoEpisodeRule`

This is a page / pagination / chapter / video rule container. It is a distinct rule domain from AdMark and from Legado chapter-text replacement. Its field names alone do not establish the exact runtime semantics of every rule type.

### Implementation consequences

1. Keep three independent adapters: WebView injected ad filtering, AdMark URL/DOM marking, and PPRule page/catalog/video processing.
2. Keep the original on/off preference separate from Legado replacement-rule enablement.
3. Implement local, user-owned ad-mark CRUD and a safe WebView adapter before attempting any optional service synchronization.
4. Do not hard-code or recreate the original private service endpoints/protocols from symbols alone. Any network integration requires a documented, authorized service contract.
5. The current project still does not contain the original WebView injection script, the complete AdMark matching/removal implementation, or the PPRule runtime. The findings above are reverse-engineering evidence, not a claim that original purification has been ported.


## Shared Legado + Badcat integration progress

The Android host now exposes a shared purification coordinator from the same
factory that builds the Legado reading engine. The two domains remain separate
inside that shared lifecycle:

- Legado source requests continue through the shared bounded HTTP gateway,
  per-source Rhino runtime, and source-isolated persistent CookieJar.
- Legado chapter title/body replacement continues through
  CatalogLegadoBookTextReplacement and the Legado replacement-rule catalog.
- Badcat WebView ad filtering has its own persisted original-style toggle
  (key_is_ad_block) and bounded injection-script setting
  (key_ad_block_js_inject). The reader WebView lifecycle calls the adapter
  after the current document finishes loading; JavaScript stays disabled unless
  the toggle is on and a non-empty bounded script is present.
- Badcat AdMark records have a separate app-private catalog with local
  upsert/remove/import/export. host, urlPattern and markRules are preserved
  as distinct fields. The current safe lookup uses exact host matching only.

This is integration plumbing, not a completed port of the original purifier.
The original injected script has not been recovered; urlPattern/markRules
matching and DOM removal semantics are still opaque; no original issue/report
service is called; the current reader only has a verified injection hook and
local AdMark storage. The AdMark payload is not yet used to remove DOM nodes.
The app must not claim full original Badcat ad filtering until the original
matcher and callback contract are recovered and validated against authorized
fixtures.

Regression coverage added for toggle-gated injection selection, exact-host
AdMark lookup, invalid record rejection and preservation of opaque rule data.
