# Runtime-1.9 — exact shipped NativeJava resolver DEX bytecode audit

Date: 2026-11-21

## 1. Provenance and exact artifact boundary

- Registration: Runtime-1.9 in docs/required/03-LEGADO-RUNTIME.md, registration commit 3601ba1288297795e37e94fac3b42b82a2934080.
- Starting code HEAD: fc5366f255b8a666887296c0d47564e47abab17f.
- Official APK: legado_app_3.26110117_universal_release_vc38521.apk; 27,857,704 bytes; SHA-256 31e5a1db1ea47508a1c8193e8e9c1b5aeb49b585317e1e8466d2851e7038e0b3. Embedded revision matches LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a.
- DEX class containing exact shipped resolver: org.htmlunit.corejs.javascript.NativeJavaMethod in classes3.dex.
- DEX class containing exact shipped conversions: org.htmlunit.corejs.javascript.NativeJavaObject in classes3.dex.
- classes3.dex SHA-256: 58d9f330130b93b521cb3854de6aab9e9d38106f92820e23cf6336a2bf6f5657.
- The exact custom JAR bytes still have not been extracted from the checked-in Maven blob. This audit instead inspects the exact post-build DEX code shipped in the verified official APK, which is the runtime code that executes in that release. It does not claim the DEX is byte-for-byte identical to the original JAR class files; R8/DEX transformation is part of the APK provenance boundary.
- Method code evidence uses DEX class_data method definitions, full method_id/proto descriptors, code_item register/ins/outs/tries fields, instruction code units, method/type/field/string references and branch targets. Method instruction code-unit SHA-256 values below allow exact code-item identification.

## 2. Exact shipped NativeJavaMethod methods

All listed methods are in classes3.dex and retain their method names/descriptors after packaging.

| Method | Descriptor | Code offset | Instruction units | Instruction SHA-256 |
|---|---|---:|---:|---|
| call | (Context, VarScope, Scriptable, Object[])Object | 5723356 | 227 | 0dc94de1d33257ba75ccdabaac5a8e3f1e84c0a8d3a6ae1a12ef150660bf89ba |
| findCachedFunction | (Context, Object[])int | 5722196 | 68 | c2e2fc9f2459b7b7e3512f167ea5e9c4a398435a885cae2071a0417b87b441ad |
| findFunction | (Context, ExecutableBox[], Object[])int | 5722348 | 294 | b7130daa3dff635880c712324c4f422add8e5ebb57d6a2cb84c8eebac698abf0 |
| failFastConversionWeights | (Object[], ExecutableBox)int[] | 5723232 | 53 | 003021dde05f5faf9527c79a9cdbab36c994375f0104b2226a0602eb00fadb57 |
| preferSignature | (Object[], ExecutableBox, int[], ExecutableBox, int[])int | 5722952 | 131 | dd90ed7c62beb824fb7f49b6f55fc89f64c041fb610f62672748bd4f37c8b8a6 |

The descriptors above are abbreviated only for readability; the full DEX descriptors are:
- call: (Lorg/htmlunit/corejs/javascript/Context;Lorg/htmlunit/corejs/javascript/VarScope;Lorg/htmlunit/corejs/javascript/Scriptable;[Ljava/lang/Object;)Ljava/lang/Object;
- findCachedFunction: (Lorg/htmlunit/corejs/javascript/Context;[Ljava/lang/Object;)I
- findFunction: (Lorg/htmlunit/corejs/javascript/Context;[Lorg/htmlunit/corejs/javascript/lc/member/ExecutableBox;[Ljava/lang/Object;)I
- failFastConversionWeights: ([Ljava/lang/Object;Lorg/htmlunit/corejs/javascript/lc/member/ExecutableBox;)[I
- preferSignature: ([Ljava/lang/Object;Lorg/htmlunit/corejs/javascript/lc/member/ExecutableBox;[ILorg/htmlunit/corejs/javascript/lc/member/ExecutableBox;[I)I

### 2.1 Dispatch and overload candidate selection

The exact shipped DEX establishes this call path:

1. NativeJavaMethod.call calls findCachedFunction with the JS argument array.
2. For an overload set, findCachedFunction scans a CopyOnWriteArrayList of ResolvedOverload entries. A matching cached argument signature returns its prior selected method index.
3. On a cache miss, findFunction is called. The overload cache is bounded to at most twice the number of candidate methods.
4. findFunction invokes failFastConversionWeights for each candidate. A non-varargs candidate requires exact argument count. A varargs candidate subtracts its final array parameter from the fixed-argument count and rejects only when fewer than the fixed parameters are supplied.
5. failFastConversionWeights computes per-argument conversion weights for the fixed parameters. A weight of 99 marks a candidate inapplicable. Varargs trailing arguments are handled in preferSignature using the final varargs element type.
6. preferSignature compares candidate parameter types and the per-argument conversion weights. Lower conversion weight wins. If weights tie at zero, TypeInfo assignability can prefer the more specific type. Equal non-zero weights are ambiguous for that argument. Conflicting preferences across arguments produce the ambiguous preference code.
7. findFunction also resolves certain static/instance and declaring-class specificity cases. If more than one best candidate remains, it raises the method-ambiguous evaluator error; it does not silently choose an arbitrary same-arity overload.
8. call uses the selected ExecutableBox, wraps/coerces arguments, searches the Scriptable receiver/prototype chain for a Wrapper whose unwrapped object is an instance of the declaring class, invokes the executable, and passes the return value through WrapFactory.wrap.

This confirms the type-sensitive, conversion-weight-based model for the exact shipped APK. It is not the Android WebView bridge's count-only overload rule.

## 3. Exact shipped conversion-weight implementation

NativeJavaObject.getConversionWeight is present in classes3.dex with descriptor:
(Lorg/htmlunit/corejs/javascript/NativeJavaObject;Ljava/lang/Object;Lorg/htmlunit/corejs/javascript/lc/type/TypeInfo;)I
(actual full static method descriptor: (Ljava/lang/Object;Lorg/htmlunit/corejs/javascript/lc/type/TypeInfo;)I)

- Code offset: 5725500
- Instruction units: 320
- Instruction SHA-256: b8d422a43154e03f1e357de3d0e1783e6053a1071194c77c6c0eb018affd0649

The DEX method uses getJSTypeCode and a packed-switch on the resulting JS value category. getJSTypeCode separately identifies undefined, null, Boolean, Number, String, Java Class, Wrapper, Java array, general Scriptable and BigInteger categories. The following weights are decoded from the exact DEX branches; 99 is the no-conversion/inapplicable sentinel.

| JS argument category | Java parameter type | Exact DEX weight/behavior |
|---|---|---|
| undefined | String or exact Object | 1; other types rejected (99) |
| null | reference type | 1 |
| null | primitive | rejected (99) |
| Boolean | primitive boolean | 1 |
| Boolean | boxed Boolean | 2 |
| Boolean | exact Object | 3 |
| Boolean | String | 4 |
| Boolean | other types | rejected (99) |
| String | String | 1 |
| String | compatible instance type | 2 |
| String | char | 3 |
| String | numeric primitive (not boolean) | 4 |
| String | boolean / other unsupported types | rejected (99) |
| Number | primitive double | 1 |
| Number | other numeric primitive | getSizeRank + 1 |
| Number | String | 9 |
| Number | BigInteger | 10 |
| Number | exact Object | 11 |
| Number | another Number type | 2 |
| Number | boolean / unsupported types | rejected (99) |
| Wrapped Java object | target accepts unwrapped instance | 0 |
| Wrapped Java object | exact Object | 2 |
| Wrapped Java object | primitive numeric | getSizeRank + 2; boolean rejected |
| Scriptable / JS object | assignable target instance | 1 |
| NativeArray | Java array target | 2 |
| Scriptable / JS object | exact Object | 3 |
| Scriptable / JS object | String | 4 |
| NativeDate | Date target | 1 |
| JSFunction | Java interface target | 1 |
| NativeObject | Java interface target | 2 |
| other JS object | Java interface target | 12 |

The exact getSizeRank(Class) DEX method returns:
- double = 1
- float = 2
- long = 3
- int = 4
- short = 5
- char = 6
- byte = 7
- boolean = 99
- other types = 8

The conversion-weight method adds different offsets depending on the JS input category. Do not quote the raw size ranks as the final conversion weights.

## 4. Exact shipped coercion implementation

NativeJavaObject.coerceTypeImpl is present in classes3.dex:
- Descriptor: (Lorg/htmlunit/corejs/javascript/lc/type/TypeInfo;Ljava/lang/Object;)Ljava/lang/Object;
- Code offset: 5727240
- Instruction units: 500
- Instruction SHA-256: ebda4d9aaf60dff2d3c958126c6eb586a591f56cc392c1f65f0c29250d092990

The DEX control flow confirms:
- null to primitive types reports conversion failure; null to reference types returns null;
- undefined maps to the literal string "undefined" for String and is accepted for exact Object; other targets fail conversion;
- JS Boolean has its own branch and supports Boolean/Object/String cases, not arbitrary numeric coercion;
- Number and BigInteger conversions pass through coerceToNumber for numeric target types;
- String conversion has a String fast path, compatible-instance path, char handling and numeric primitive coercion;
- NativeDate can be converted to Date;
- NativeArray can be allocated as a Java array and each element recursively passed through coerceTypeImpl;
- Wrapper objects are unwrapped and checked against the requested target;
- supported Java interface targets can be adapted from JS functions/objects through createInterfaceAdapter;
- incompatible values reach reportConversionError rather than silently becoming arbitrary Java values.

The exact helper methods are present in the same DEX class:
- getJSTypeCode(Object) code offset 5726156, 77 instruction units, SHA-256 110e733c1ee4de212182973594a36b9f3fd321bf7063c53b75206fb33640ba53.
- coerceToNumber(TypeInfo,Object) code offset 5726568, 307 instruction units, SHA-256 6a8db89d64d7959ce8b8e71a061c2dadddd7a2c6e45af8bfc1fe6150870af5ab.
- getSizeRank(Class) code offset 5726328, 52 instruction units, SHA-256 44d0c8926ddf1f122849b40f196be1b38a1f0a89eeca9ee1858d8c22801b30c4.
- getSizeRank(TypeInfo) code offset 5726448, 18 instruction units, SHA-256 43ff1372d97dfc117ce628ac7e71d89d997167f88ce2788dfcf154ba03b625d2.

## 5. Concrete consequence for base64Decode

The pinned Legado source and APK DEX expose these two annotated two-argument overloads:
- base64Decode(String, int)
- base64Decode(String, String)

The exact shipped NativeJava resolver distinguishes their second parameter by JS value type:
- For a JS String argument: String target has weight 1; numeric primitive target has weight 4. The String overload wins.
- For a JS Number argument: primitive int receives getSizeRank(int)+1 = 5; String receives weight 9. The int overload wins.
- For null: reference String is applicable with weight 1; primitive int is rejected.
- For undefined: String is accepted with weight 1; primitive int is rejected.

Android WebView addJavascriptInterface is different: its documented overload dispatch uses argument count only; same-arity overload selection may be arbitrary and ignores JS argument types. Thus a source that relies on the correct Rhino overload may behave differently when the same object is exposed through WebView. This is a concrete cross-runtime semantic mismatch, not an abstract possibility.

## 6. Comparison with public HtmlUnit 5.3.0 source

The exact shipped DEX findings agree with the high-level public 5.3.0 source-family model documented in Runtime-1.7:
- conversion-weight candidate ranking;
- fail-fast arity and varargs checks;
- assignability tie-breaking for zero-weight matches;
- ambiguous-method error when no unique best candidate exists;
- JS type-specific conversion and coercion.

This confirms the shipped APK behavior for the audited methods even though the checked-in custom JAR blob was not extracted. It does not establish a byte-for-byte JAR/source match or certify every method in the HtmlUnit dependency. The exact APK runtime class is the primary evidence for the behavior described here.

## 7. Acceptance matrix and decision

| Requirement | Result | Reason |
|---|---|---|
| Exact official APK provenance | PASS | SHA-256 and embedded pinned upstream revision verified |
| Exact shipped NativeJavaMethod overload path | PASS | call/findCachedFunction/findFunction/failFastConversionWeights/preferSignature DEX code extracted |
| Exact shipped NativeJavaObject conversion weights | PASS | getConversionWeight/getJSTypeCode/getSizeRank DEX code extracted |
| Exact shipped coercion behavior | PASS (audited branches) | coerceTypeImpl/coerceToNumber DEX code and call references inspected |
| Public 5.3.0 source-family cross-check | PASS | high-level algorithm agrees with exact shipped DEX |
| Exact custom JAR byte-for-byte source mapping | NOT REQUIRED FOR APK RUNTIME CLAIM; STILL UNAVAILABLE | shipped APK DEX is primary evidence; original checked-in JAR bytes were not extracted |
| All dependency methods / every engine feature | PARTIAL | this task audited the resolver/conversion methods needed for overload dispatch, not every class/method in CoreJS |
| Differential JS execution | NOT RUN | no runtime probe was authorized |
| WebView same-arity overload behavior | STATIC PLATFORM CONTRACT | exact device/provider execution remains untested |

**Decision: GO to design a separately registered, bounded differential probe for the exact shipped Rhino overload behavior and the WebView base64Decode collision. NO-GO for production bridge integration or any full compatibility claim.** The key runtime uncertainty from Runtime-1.7 is now closed for the APK's NativeJava resolver methods. The next test must use a provenance-matched runtime and compare Rhino versus WebView separately; no code changes should be bundled into that probe.

## 8. Validation and actual changes

- Recomputed official APK SHA-256; parsed exact DEX method/class/proto/code_item records from classes3.dex.
- Decoded instruction streams and method references for NativeJavaMethod and NativeJavaObject; recorded instruction-unit hashes for key methods.
- No DEX bytecode was executed; no JavaScript was evaluated; no Gradle/build/test/CI/device/compiler experiment/APK rebuild was performed.
- No production source, tests, Gradle/workflow, DB/schema/migration/persistence, caller, UI/Reader or main changed.
- Only this report and the Runtime checklist result are authorized.
- Runtime-1.9 resolver/conversion audit: **PASS for the audited shipped APK methods**. Overall Runtime remains partial; G2 is still blocked/partial; B′ and full Legado compatibility remain unpassed.
