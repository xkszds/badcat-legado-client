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
- JavaScript @js replacement expressions: explicitly unsupported and fail closed.
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
