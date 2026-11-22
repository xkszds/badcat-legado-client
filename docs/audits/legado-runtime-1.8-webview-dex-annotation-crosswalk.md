# Runtime-1.8 — pinned APK WebView annotation and override crosswalk

Date: 2026-11-21

## 1. Provenance and parser scope

- Registration: Runtime-1.8 in docs/required/03-LEGADO-RUNTIME.md, registration commit 5d16f65f748cdfcbc8b6e5aeaebc24c2daf8c3db.
- Starting code HEAD: b092ccf1de4593effdd28521d36002373090d642.
- APK: legado_app_3.26110117_universal_release_vc38521.apk, 27,857,704 bytes, SHA-256 31e5a1db1ea47508a1c8193e8e9c1b5aeb49b585317e1e8466d2851e7038e0b3; embedded META-INF/version-control-info.textproto revision 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- Parsed all three DEX files from the verified APK. The parser reads DEX header string/type/proto/method/class tables, class definitions, encoded class data and method access flags, annotation directories, annotation-set items and runtime annotation items. For each class it resolves method index deltas separately for direct and virtual method lists, then joins method annotation directories to exact method IDs and complete parameter/return descriptors. It also reads class supertype and interface lists.
- Every android.webkit.JavascriptInterface annotation item observed on the audited classes has DEX visibility RUNTIME (value 1). Every annotated method in these four relevant classes is public; none is static/private/protected. This is DEX metadata evidence, not an Android device execution.
- The parser output reconciles prior class method totals: WebJsExtensions 67, RssJsExtensions 190, JsExtensions 148, SourceLoginJsExtensions 12.

## 2. Exact DEX hierarchy and method counts

| DEX class descriptor | Source identity | Class relationship | Defined methods | Public | Direct runtime JavascriptInterface methods |
|---|---|---|---:|---:|---:|
| Lio/legado/app/help/webView/WebJsExtensions; | WebJsExtensions.kt | extends RssJsExtensions | 67 | 54 | 18 |
| Lio/legado/app/ui/rss/read/RssJsExtensions; | RssJsExtensions.kt | implements JsExtensions | 190 | 185 | 83 |
| Lio/legado/app/help/JsExtensions; | JsExtensions.kt | interface; extends obfuscated Lio/e1; | 148 | 148 | 68 |
| Lio/e1; | source JsEncodeUtils.kt, matched by source/interface hierarchy and method signatures | parent interface of JsExtensions | 22 | 22 | 20 |
| Lio/legado/app/ui/login/SourceLoginJsExtensions; | SourceLoginJsExtensions.kt | extends RssJsExtensions | 12 | 12 | 0 |

The obfuscated Lio/e1; identity is mapped to source JsEncodeUtils using the pinned source declaration JsExtensions : JsEncodeUtils, the DEX interface edge and exact method signature overlap; DEX itself only names it Lio/e1;.

Pinned source blobs:
- WebJsExtensions.kt: dd6532396b8f02e37376885aa2c6c1e430af1ad9
- RssJsExtensions.kt: 8e01035e8d5802ed931ac46050cd45cfb0254701
- JsExtensions.kt: f6808edd2a0d312e8bd13649935dea444cb82581
- JsEncodeUtils.kt: c75b61650db2abdd64a09d2bd8340ef14c2947b3

## 3. Corrected annotation counts and compiler-generated members

A direct count of JavascriptInterface tokens in the pinned Kotlin source gives:
- WebJsExtensions.kt: 12 source annotations.
- RssJsExtensions.kt: 10 source annotations.
- JsExtensions.kt: 48 source annotations.
- JsEncodeUtils.kt: 20 source annotations.

The APK DEX runtime annotation directories give 18, 83, 68 and 20 respectively on the corresponding classes/interfaces. The difference is not evidence of extra source declarations; DEX includes compiler-generated/bridge method entries and interface implementation stubs. Relevant access-flag findings:
- WebJsExtensions: 18 annotated methods, all ordinary (no ACC_BRIDGE / ACC_SYNTHETIC flags).
- RssJsExtensions: 83 annotated methods; 67 carry ACC_BRIDGE.
- JsExtensions: 68 annotated methods; 20 carry ACC_BRIDGE and ACC_SYNTHETIC.
- JsEncodeUtils (Lio/e1;): 20 annotated methods; none marked bridge/synthetic.

**Correction to earlier Runtime-1.5/1.6 audit wording:** the previously recorded 12 / 64 / 54 values for Web/RSS/base were not the complete DEX direct runtime annotation-directory counts. The reproducible DEX parser used here finds 18 / 83 / 68. The earlier figures must not be used as the APK's total annotated-method counts. This result supersedes those counts for the inspected official APK. The older report file itself was not changed in this registered scope.

## 4. Interface implementation and inheritance crosswalk

Exact method identity is (name, full JVM descriptor), not name alone.

| Relationship | Exact DEX result |
|---|---|
| JsExtensions interface → RssJsExtensions implementation | 148 / 148 interface method signatures have a matching RssJsExtensions method signature |
| JsEncodeUtils (Lio/e1;) → RssJsExtensions | 22 / 22 parent-interface signatures have a matching RssJsExtensions method signature |
| JsExtensions annotated interface methods → Rss implementation | all 68 annotated interface signatures have a matching directly annotated Rss method |
| WebJsExtensions → RssJsExtensions | only one exact method signature overlap: getSourceNavigationContext()Llu/h;; neither declaration has JavascriptInterface |
| Web/Rss annotated signature overlap | zero; the 18 Web and 83 Rss annotated method signatures are distinct |

Therefore the WebJsExtensions instance's statically eligible annotated public method candidate set, under the documented Android contract, is:
- 18 methods declared on WebJsExtensions; plus
- 83 non-overridden methods declared on RssJsExtensions;
- total **101 distinct annotated public JVM method signatures**.

The 68 annotated JsExtensions interface signatures and 20 parent-interface signatures are not added again: they are represented by matching annotated RssJsExtensions implementation entries. The only Web/Rss signature overlap is unannotated in both. This is a static candidate surface from DEX hierarchy/annotation evidence; it is not a claim that every method was invoked successfully on a device.

SourceLoginJsExtensions has zero directly annotated methods of its own. It is not one of the six registered WebJsExtensions objects inventoried in Runtime-1.6; its superclass relationship alone does not establish that it is injected into a WebView.

## 5. Per-method exposed candidate inventory

The following CSV contains all 101 distinct annotated public method signatures on the registered WebJsExtensions object, derived from DEX method IDs/prototypes and direct runtime annotation directories. All listed methods have runtime-visible annotations. Rss methods with ACC_BRIDGE remain separate JVM signatures and are retained in this inventory.

~~~csv
owner,name,descriptor
WebJsExtensions,ajax,(Ljava/lang/String;)Ljava/lang/String;
WebJsExtensions,ajax,(Ljava/lang/String;I)Ljava/lang/String;
WebJsExtensions,connect,(Ljava/lang/String;)Ljava/lang/String;
WebJsExtensions,connect,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
WebJsExtensions,connect,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/String;
WebJsExtensions,get,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
WebJsExtensions,get,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/String;
WebJsExtensions,getString,(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;
WebJsExtensions,getStringList,(Ljava/lang/String;Ljava/lang/String;Z)Ljava/util/List;
WebJsExtensions,head,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
WebJsExtensions,head,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/String;
WebJsExtensions,log,(Ljava/lang/String;)Ljava/lang/String;
WebJsExtensions,longToast,(Ljava/lang/String;)V
WebJsExtensions,post,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
WebJsExtensions,post,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/String;
WebJsExtensions,request,(Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)V
WebJsExtensions,toast,(Ljava/lang/String;)V
WebJsExtensions,upConfig,(Ljava/lang/String;)V
RssJsExtensions,HMacBase64,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,HMacHex,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,addBook,(Ljava/lang/String;)V
RssJsExtensions,aesBase64DecodeToString,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,aesDecodeArgsBase64Str,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,aesDecodeToString,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,aesEncodeArgsBase64Str,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,aesEncodeToBase64String,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,aesEncodeToString,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,androidId,()Ljava/lang/String;
RssJsExtensions,base64Decode,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,base64Decode,(Ljava/lang/String;I)Ljava/lang/String;
RssJsExtensions,base64Decode,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,base64Encode,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,base64Encode,(Ljava/lang/String;I)Ljava/lang/String;
RssJsExtensions,cacheContent,(Ljava/lang/Object;Ljava/lang/String;)Z
RssJsExtensions,cacheFile,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,cacheFile,(Ljava/lang/String;I)Ljava/lang/String;
RssJsExtensions,deleteFile,(Ljava/lang/String;)Z
RssJsExtensions,desBase64DecodeToString,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,desDecodeToString,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,desEncodeToBase64String,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,desEncodeToString,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,digestBase64Str,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,digestHex,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,downloadFile,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,downloadFile,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,encodeURI,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,encodeURI,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,get,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,get7zStringContent,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,get7zStringContent,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,getCookie,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,getCookie,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,getRarStringContent,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,getRarStringContent,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,getReadBookConfig,()Ljava/lang/String;
RssJsExtensions,getString,(Ljava/lang/String;Z)Ljava/lang/String;
RssJsExtensions,getThemeConfig,()Ljava/lang/String;
RssJsExtensions,getThemeMode,()Ljava/lang/String;
RssJsExtensions,getTxtInFolder,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,getWebViewUA,()Ljava/lang/String;
RssJsExtensions,getZipStringContent,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,getZipStringContent,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,hexDecodeToString,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,hexEncodeToString,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,htmlFormat,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,htmlFormat,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,importScript,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,md5Encode,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,md5Encode16,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,open,(Ljava/lang/String;)V
RssJsExtensions,open,(Ljava/lang/String;Ljava/lang/String;)V
RssJsExtensions,open,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
RssJsExtensions,open,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
RssJsExtensions,openUrl,(Ljava/lang/String;)V
RssJsExtensions,openUrl,(Ljava/lang/String;Ljava/lang/String;)V
RssJsExtensions,openVideoPlayer,(Ljava/lang/String;Ljava/lang/String;)V
RssJsExtensions,openVideoPlayer,(Ljava/lang/String;Ljava/lang/String;Z)V
RssJsExtensions,put,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,randomUUID,()Ljava/lang/String;
RssJsExtensions,readTxtFile,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,readTxtFile,(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,s2t,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,searchBook,(Ljava/lang/String;)V
RssJsExtensions,searchBook,(Ljava/lang/String;Ljava/lang/String;)V
RssJsExtensions,setBaseUrl,(Ljava/lang/String;)Lio/legado/app/model/analyzeRule/AnalyzeRule;
RssJsExtensions,setContent,(Ljava/lang/Object;)Lio/legado/app/model/analyzeRule/AnalyzeRule;
RssJsExtensions,setContent,(Ljava/lang/Object;Ljava/lang/String;)Lio/legado/app/model/analyzeRule/AnalyzeRule;
RssJsExtensions,setRedirectUrl,(Ljava/lang/String;)Ljava/net/URL;
RssJsExtensions,showPhoto,(Ljava/lang/String;)V
RssJsExtensions,t2s,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,timeFormat,(J)Ljava/lang/String;
RssJsExtensions,timeFormatUTC,(JLjava/lang/String;I)Ljava/lang/String;
RssJsExtensions,toNumChapter,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,tripleDESDecodeArgsBase64Str,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,tripleDESDecodeStr,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,tripleDESEncodeArgsBase64Str,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,tripleDESEncodeBase64Str,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,un7zFile,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,unArchiveFile,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,unrarFile,(Ljava/lang/String;)Ljava/lang/String;
RssJsExtensions,unzipFile,(Ljava/lang/String;)Ljava/lang/String;
~~~

## 6. Material WebView overload collision

The registered WebJsExtensions object inherits two directly annotated, public, same-name/same-arity methods from RssJsExtensions:

| Method | Descriptor | Second parameter |
|---|---|---|
| base64Decode | (Ljava/lang/String;I)Ljava/lang/String; | primitive int |
| base64Decode | (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; | String |

The pinned source declares both as JavascriptInterface overloads in JsExtensions.kt (around lines 676–686). Both signatures are directly annotated in the APK DEX implementation/interface metadata. A third one-argument base64Decode(String) overload is also annotated but has a distinct argument count.

Under the Android WebView contract recorded in Runtime-1.7, overload dispatch uses argument count only; it does not use JS argument types, and one of multiple same-arity overloads may be selected arbitrarily. Therefore JavaScript cannot safely rely on a String second argument selecting the String overload or a numeric second argument selecting the int overload. This is a concrete API ambiguity on a registered WebView bridge surface, independent of Rhino's type-sensitive overload-ranking model.

Other same-name overload groups in the audited annotated surface have distinct argument counts. The parser found no other same-name/same-arity pair in WebJsExtensions or RssJsExtensions.

## 7. Platform contract applied narrowly

The supported app boundary remains minSdk 23 / targetSdk 36. Per Android's published current contract, public methods with runtime JavascriptInterface annotations are exposed; non-overridden annotated superclass methods remain eligible; an override must repeat the annotation. The DEX crosswalk finds no annotated Web method overriding an annotated Rss method. It finds one unannotated Web/Rss overlap, which is not an exposed method.

This inventory does not prove:
- a specific WebView provider/version's exact reflection implementation;
- that any individual JS call was made or succeeded;
- dynamic calls hidden in remote/source-provided scripts;
- overload behavior of the pinned HtmlUnit 5.3.0-legado.4 engine;
- that the WebView and Rhino bridge have equivalent semantics.

## 8. Acceptance matrix and decision

| Requirement | Result | Reason |
|---|---|---|
| Verified pinned APK provenance | PASS | SHA-256 and embedded upstream revision verified |
| Exact class hierarchy/interface edges | PASS | DEX class_def super/interface lists parsed and matched to pinned source |
| Per-method DEX descriptors and direct runtime annotation visibility | PASS | 101 unique public annotated method signatures inventoried for WebJsExtensions + RssJsExtensions |
| Generated/bridge methods retained | PASS | bridge/synthetic flags included; prior undercounts corrected |
| Interface methods deduplicated against concrete implementations | PASS | exact 148/148 and 22/22 signature crosswalks; no interface duplicate added to final 101 |
| WebView inherited/override exposure conclusion | PASS (static contract) | source hierarchy + DEX signatures/annotations reconciled with Android published contract |
| Same-arity overload ambiguity | PASS (static collision identified) | annotated base64Decode(String,int) and base64Decode(String,String) share arity 2 |
| Provider/device behavior | NOT RUN | no device test or provider-specific instrumentation |
| Exact pinned HtmlUnit .4 resolver parity | BLOCKED | remains Runtime-1.7 gap; this task does not inspect the dependency JAR |

**Decision: WebView annotation/exposure metadata crosswalk is locally accepted as a static APK evidence inventory; overall Runtime-1.8 remains PARTIAL / ACCEPTANCE NOT MET.** The base64Decode overload collision is a concrete WebView compatibility risk. Do not claim full Legado compatibility, do not treat WebView overloads as Rhino overloads, and do not proceed to production integration based on this metadata audit alone.

Next permitted action: separately register an isolated compatibility decision/probe design for the base64Decode WebView same-arity overload and a method-specific bridge contract inventory. Exact .4 HtmlUnit binary inspection remains a separate unresolved gate.

## 9. Validation and actual changes

- Recomputed official APK SHA-256 and confirmed the previously recorded digest.
- Parsed all three DEX files and extracted the relevant class hierarchy, method prototypes, direct runtime annotations, visibility and bridge/synthetic flags.
- Compared exact descriptors across JsExtensions, its parent interface, RssJsExtensions and WebJsExtensions.
- Compared source annotation token counts for the four pinned Kotlin source files against DEX method annotation entries.
- No build, compile, Gradle, tests, CI, runtime execution, device test, compiler experiment or APK rebuild was performed.
- No production code, tests, Gradle/workflow, DB/schema/migration, persistence, callers, UI/Reader or main changed.
- Only this audit report and the Runtime checklist result are authorized.
- Runtime-1.8 static WebView annotation crosswalk: PASS. Runtime-1.8 overall: PARTIAL / ACCEPTANCE NOT MET due no device/provider test and unresolved exact HtmlUnit .4 binary semantics. G2 remains BLOCKED/partial; B′ and full Legado compatibility remain unpassed.
