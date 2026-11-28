#!/usr/bin/env bash
set -euo pipefail

repo_root="$GITHUB_WORKSPACE"
test -n "$repo_root"
legado_root="$repo_root/upstream/legado"
out="$repo_root/build/pinned-upstream-abi-proof"
mkdir -p "$out"

expected_badcat="$GITHUB_SHA"
expected_legado="330e606c4d57e08adc056ff8ab7a4132c784da9a"
test -n "$expected_badcat"
test "$(git -C "$repo_root" rev-parse HEAD)" = "$expected_badcat"
test "$(git -C "$legado_root" rev-parse HEAD)" = "$expected_legado"
test "$(cat "$out/compile-result.txt")" = "RESULT=COMPILE_PASS"
test -s "$out/javap/BaseSource.javap.txt"
test -s "$out/javap/JsExtensions.javap.txt"

cat > "$out/abi-classpath.init.gradle" <<'GRADLE'
gradle.projectsEvaluated {
    def app = gradle.rootProject.project(':app')
    app.tasks.register('writePinnedAbiCompileClasspath') {
        doLast {
            def configuration = app.configurations.getByName('appDebugCompileClasspath')
            def artifactType = org.gradle.api.attributes.Attribute.of('artifactType', String)
            def classpathView = configuration.incoming.artifactView {
                attributes {
                    attribute(artifactType, 'android-classes-jar')
                }
            }
            def output = new File(System.getenv('GITHUB_WORKSPACE') + '/build/pinned-upstream-abi-proof/compile-classpath.txt')
            output.parentFile.mkdirs()
            output.text = classpathView.files.asPath
            println("PINNED_ABI_CLASSPATH_ENTRIES=" + classpathView.files.size())
        }
    }
}
GRADLE

set +e
(
    cd "$legado_root"
    gradle --no-daemon --offline --console plain --max-workers=2 \
        -Dorg.gradle.parallel=false \
        -Dorg.gradle.jvmargs="-Xmx1g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8" \
        -Pkotlin.compiler.execution.strategy=in-process \
        -I "$out/abi-classpath.init.gradle" \
        :app:writePinnedAbiCompileClasspath
) > "$out/reflection-classpath-gradle.log" 2>&1
gradle_status=$?
set -e
cat "$out/reflection-classpath-gradle.log"
if [[ "$gradle_status" -ne 0 ]]; then
    echo "ABI_CLASSPATH_RESOLUTION=FAILED" | tee "$out/reflection-classpath-summary.txt"
    exit "$gradle_status"
fi
test -s "$out/compile-classpath.txt"
echo "ABI_CLASSPATH_RESOLUTION=PASS" | tee "$out/reflection-classpath-summary.txt"

android_jar="$ANDROID_HOME/platforms/android-36/android.jar"
test -s "$android_jar"
app_classes="$legado_root/app/build/tmp/kotlin-classes/appDebug"
test -d "$app_classes"
app_javac_classes="$legado_root/app/build/intermediates/javac/appDebug/compileAppDebugJavaWithJavac/classes"
test -d "$app_javac_classes"
test -s "$app_javac_classes/io/legado/app/model/analyzeRule/QueryTTF.class"
sha256sum "$app_javac_classes/io/legado/app/model/analyzeRule/QueryTTF.class" | tee "$out/queryttf-class-sha256.txt"

classpath="$(cat "$out/compile-classpath.txt")"
classpath="$classpath:$android_jar:$app_classes:$app_javac_classes"
for classes_dir in "$legado_root"/app/build/intermediates/javac/appDebug/*/classes; do
    if [[ -d "$classes_dir" ]]; then classpath="$classpath:$classes_dir"; fi
done
for module_jar in "$legado_root"/modules/*/build/intermediates/compile_library_classes_jar/debug/*/classes.jar; do
    if [[ -f "$module_jar" ]]; then classpath="$classpath:$module_jar"; fi
done
printf 'CLASSPATH_ENTRY_COUNT=%s\n' "$(tr ':' '\n' <<< "$classpath" | wc -l)" | tee "$out/reflection-classpath-summary.txt"
printf 'ANDROID_JAR=%s\nAPP_CLASSES=%s\nAPP_JAVAC_CLASSES=%s\n' "$android_jar" "$app_classes" "$app_javac_classes" >> "$out/reflection-classpath-summary.txt"
sha256sum "$out/compile-classpath.txt" "$out/abi-classpath.init.gradle" > "$out/reflection-classpath-SHA256SUMS.txt"

cat > "$out/PinnedUpstreamAbiReflectionProbe.java" <<'JAVA'
import android.webkit.JavascriptInterface;
import com.script.ScriptBindings;
import io.legado.app.data.entities.BaseSource;
import io.legado.app.help.rhino.NativeBaseSource;
import org.htmlunit.corejs.javascript.Context;
import org.htmlunit.corejs.javascript.Function;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Comparator;

public final class PinnedUpstreamAbiReflectionProbe {
    private static final String[] TARGETS = {
        "io.legado.app.data.entities.BaseSource",
        "io.legado.app.model.analyzeRule.AnalyzeRule",
        "io.legado.app.model.analyzeRule.AnalyzeUrl",
        "io.legado.app.help.JsExtensions",
        "io.legado.app.help.JsEncodeUtils",
        "io.legado.app.help.RegexJsExtensions",
        "io.legado.app.model.jsSource.JsSourceEngine",
        "io.legado.app.data.entities.BookSource",
        "io.legado.app.data.entities.Book",
        "io.legado.app.data.entities.BookChapter",
        "io.legado.app.model.analyzeRule.RuleData",
        "io.legado.app.model.analyzeRule.RuleDataInterface",
        "io.legado.app.model.BatchContentContext",
        "io.legado.app.help.book.ContentSaveToken",
        "io.legado.app.help.book.BookHelp",
        "io.legado.app.data.entities.BaseBook",
        "io.legado.app.data.entities.Book$ReadConfig",
        "io.legado.app.data.entities.Book$ParagraphAnchor",
        "io.legado.app.data.entities.Book$Converters",
        "io.legado.app.data.entities.BookSource$Converters",
        "io.legado.app.data.entities.rule.BookListRule",
        "io.legado.app.data.entities.rule.ExploreRule",
        "io.legado.app.data.entities.rule.SearchRule",
        "io.legado.app.data.entities.rule.BookInfoRule",
        "io.legado.app.data.entities.rule.TocRule",
        "io.legado.app.data.entities.rule.ContentRule",
        "io.legado.app.data.entities.rule.ReviewRule",
        "io.legado.app.help.book.ContentSaveKey",
        "io.legado.app.help.book.ContentSaveState",
        "io.legado.app.help.book.ChapterSourceMatch",
        "io.legado.app.help.book.ChapterSourceMatch$Unique",
        "io.legado.app.help.book.ChapterSourceMatch$Ambiguous",
        "io.legado.app.help.book.ChapterSourceMatch$Missing",
        "io.legado.app.help.book.ContentSaveFence",
        "io.legado.app.help.book.PendingResourceChapter",
        "io.legado.app.utils.GsonExtensionsKt",
        "io.legado.app.utils.StringJsonDeserializer",
        "io.legado.app.utils.IntJsonDeserializer",
        "io.legado.app.utils.MapDeserializerDoubleAsIntFix",
        "io.legado.app.utils.LocalDateTypeAdapter"
    };

    public static void main(String[] args) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        int totalDeclared = 0;
        int totalPublic = 0;
        int totalSynthetic = 0;
        int totalJsAnnotated = 0;

        for (String name : TARGETS) {
            Class<?> type = Class.forName(name, false, loader);
            Method[] declared = type.getDeclaredMethods();
            Arrays.sort(declared, Comparator.comparing(Method::getName)
                .thenComparing(m -> MethodType.methodType(m.getReturnType(), m.getParameterTypes())
                    .toMethodDescriptorString()));
            int publicCount = 0;
            int syntheticCount = 0;
            int bridgeCount = 0;
            int jsCount = 0;
            for (Method method : declared) {
                boolean isPublic = java.lang.reflect.Modifier.isPublic(method.getModifiers());
                boolean js = method.isAnnotationPresent(JavascriptInterface.class);
                if (isPublic) publicCount++;
                if (method.isSynthetic()) syntheticCount++;
                if (method.isBridge()) bridgeCount++;
                if (js) jsCount++;
                String descriptor = MethodType.methodType(method.getReturnType(), method.getParameterTypes())
                    .toMethodDescriptorString();
                System.out.println("REFLECT_METHOD\t" + name + "\t" + method.getName() + "\t" + descriptor
                    + "\tpublic=" + isPublic + "\tsynthetic=" + method.isSynthetic()
                    + "\tbridge=" + method.isBridge() + "\tdefault=" + method.isDefault()
                    + "\tJavascriptInterface=" + js);
            }
            int inheritedPublic = type.getMethods().length;
            System.out.println("REFLECT_CLASS\t" + name + "\tdeclared=" + declared.length
                + "\tpublicDeclared=" + publicCount + "\tinheritedPublic=" + inheritedPublic
                + "\tsynthetic=" + syntheticCount + "\tbridge=" + bridgeCount
                + "\tJavascriptInterface=" + jsCount);
            boolean permittedMarker = name.equals("io.legado.app.help.book.ChapterSourceMatch");
            if ((declared.length == 0 && !permittedMarker)
                || (declared.length > 0 && publicCount == 0)) {
                throw new AssertionError("No usable declared methods found for " + name);
            }
            totalDeclared += declared.length;
            totalPublic += publicCount;
            totalSynthetic += syntheticCount;
            totalJsAnnotated += jsCount;
        }

        System.out.println("REFLECT_TOTAL declared=" + totalDeclared + " publicDeclared=" + totalPublic
            + " synthetic=" + totalSynthetic + " JavascriptInterface=" + totalJsAnnotated);
        System.out.println("JAVA_REFLECTION_RESULT=PASS");

        Context context = Context.enter();
        try {
            ScriptBindings scope = new ScriptBindings();
            InvocationHandler handler = (proxy, method, methodArgs) -> {
                if (method.getDeclaringClass() == Object.class) {
                    if (method.getName().equals("toString")) return "Pinned ABI BaseSource proxy";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == methodArgs[0];
                }
                if (method.getName().equals("getTag")) return "abi-probe";
                if (method.getName().equals("getKey")) return "abi-probe-key";
                if (method.getName().equals("getLoginUrl")) return "https://example.invalid";
                if (method.getName().equals("getLoginUi")) return "{}";
                if (method.getName().equals("getHeader")) return "{}";
                if (method.getName().equals("getJsLib")) return "";
                Class<?> result = method.getReturnType();
                if (result == boolean.class) return false;
                if (result == int.class) return 0;
                if (result == long.class) return 0L;
                if (result == float.class) return 0F;
                if (result == double.class) return 0D;
                if (result == short.class) return (short) 0;
                if (result == byte.class) return (byte) 0;
                if (result == char.class) return (char) 0;
                return null;
            };
            BaseSource proxy = (BaseSource) Proxy.newProxyInstance(
                BaseSource.class.getClassLoader(), new Class<?>[]{BaseSource.class}, handler);
            NativeBaseSource wrapped = new NativeBaseSource(scope, proxy, BaseSource.class);

            String[][] overloadChecks = {
                {"ajax", "2"},
                {"connect", "3"},
                {"webView", "2"},
                {"getZipStringContent", "2"},
                {"digestHex", "1"}
            };
            for (String[] check : overloadChecks) {
                String name = check[0];
                int expected = Integer.parseInt(check[1]);
                if (!wrapped.has(name, wrapped)) {
                    throw new AssertionError("Rhino JavaMembers does not expose " + name);
                }
                Object property = wrapped.get(name, wrapped);
                if (!(property instanceof Function)) {
                    throw new AssertionError("Rhino property is not Function: " + name + " -> " + property);
                }
                int actual = nativeJavaMethodOverloadCount(property);
                System.out.println("RHINO_FUNCTION\t" + name + "\tclass=" + property.getClass().getName()
                    + "\toverloads=" + actual);
                if (actual != expected) {
                    throw new AssertionError("Unexpected Rhino overload count for " + name
                        + ": expected=" + expected + " actual=" + actual);
                }
            }

            String[] required = {
                "getTag", "getKey", "getSource", "evalLoginUiV2", "evalLoginActionV2",
                "getLoginHeader", "putLoginInfo", "getVariable", "setVariable",
                "refreshJSLib", "evalJS", "cacheContent", "getWebViewUA"
            };
            for (String name : required) {
                if (!wrapped.has(name, wrapped) || !(wrapped.get(name, wrapped) instanceof Function)) {
                    throw new AssertionError("Required Rhino BaseSource/JsExtensions function missing: " + name);
                }
                System.out.println("RHINO_REQUIRED_FUNCTION\t" + name + "\tPASS");
            }
            if (!wrapped.has("loginUrl", wrapped)) {
                throw new AssertionError("Expected Java bean property loginUrl is missing");
            }
            if (wrapped.has("setLoginUrl", wrapped)) {
                throw new AssertionError("NativeBaseSource must hide setter when bean property exists");
            }
            if (!wrapped.has("setVariable", wrapped)) {
                throw new AssertionError("NativeBaseSource must preserve setVariable exception");
            }
            if (!wrapped.has("variable", wrapped)) {
                throw new AssertionError("NativeBaseSource variable property is missing");
            }
            System.out.println("RHINO_NATIVE_BASE_SOURCE_FILTER\tloginUrl=visible,setLoginUrl=hidden,setVariable=visible,variable=visible");
        } finally {
            Context.exit();
        }
        System.out.println("RESULT=JAVA_REFLECTION_AND_RHINO_JAVAMEMBERS_PASS");
    }

    private static int nativeJavaMethodOverloadCount(Object function) throws Exception {
        Object nativeMethod = findNativeJavaMethod(function, 0);
        if (nativeMethod == null) {
            throw new AssertionError("Rhino NativeJavaMethod delegate not found behind "
                + function.getClass().getName());
        }
        StringBuilder layout = new StringBuilder();
        for (Class<?> current = nativeMethod.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                Object value = null;
                String valueDescription = "unreadable";
                try {
                    field.setAccessible(true);
                    value = field.get(nativeMethod);
                    if (value == null) {
                        valueDescription = "null";
                    } else if (value.getClass().isArray()) {
                        int length = Array.getLength(value);
                        String component = value.getClass().getComponentType().getName();
                        valueDescription = "array<" + component + ">[" + length + "]";
                        if (field.getName().equals("methods")
                            && component.endsWith(".ExecutableBox") && length > 0) {
                            StringBuilder entries = new StringBuilder();
                            for (int i = 0; i < length; i++) {
                                Object executableBox = Array.get(value, i);
                                if (executableBox == null
                                    || !executableBox.getClass().getName().equals(component)) {
                                    throw new AssertionError("Unexpected Rhino methods[] entry at index "
                                        + i + ": " + (executableBox == null ? "null" : executableBox.getClass().getName()));
                                }
                                Class<?> boxType = executableBox.getClass();
                                StringBuilder boxMembers = new StringBuilder();
                                for (Field boxField : boxType.getDeclaredFields()) {
                                    boxMembers.append(boxField.getName()).append(":")
                                        .append(boxField.getType().getName()).append(",");
                                }
                                entries.append("[").append(i).append("]=")
                                    .append(boxType.getName()).append("{").append(boxMembers).append("};");
                            }
                            System.out.println("RHINO_EXECUTABLE_BOX_ENTRIES\t" + entries);
                            System.out.println("RHINO_OVERLOAD_TABLE\tfield=methods,component="
                                + component + ",count=" + length);
                            return length;
                        }
                        if (component.endsWith("MemberBox")) return length;
                        if (component.equals("java.lang.Object") && length > 0) {
                            boolean allMemberBoxes = true;
                            for (int i = 0; i < length; i++) {
                                Object item = Array.get(value, i);
                                if (item == null || !item.getClass().getSimpleName().equals("MemberBox")) {
                                    allMemberBoxes = false;
                                    break;
                                }
                            }
                            if (allMemberBoxes) return length;
                        }
                    } else {
                        valueDescription = value.getClass().getName();
                    }
                } catch (Throwable error) {
                    valueDescription = "ERROR:" + error.getClass().getName() + ":" + error.getMessage();
                }
                layout.append(current.getName()).append("#").append(field.getName())
                    .append(":").append(field.getType().getName())
                    .append("=").append(valueDescription).append(";");
            }
        }
        System.out.println("RHINO_NATIVE_JAVA_METHOD_LAYOUT\t" + layout);
        throw new AssertionError("Rhino overload table not found; actual NativeJavaMethod field layout emitted above for "
            + nativeMethod.getClass().getName());
    }

    private static Object findNativeJavaMethod(Object value, int depth) throws Exception {
        if (value == null || depth > 5) return null;
        Class<?> type = value.getClass();
        if (type.getName().endsWith(".NativeJavaMethod")) return value;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Field[] fields = current.getDeclaredFields();
            Arrays.sort(fields, Comparator.comparing((Field f) -> !f.getName().equals("function"))
                .thenComparing(Field::getName));
            for (Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    || field.getType().isPrimitive() || field.getType().isArray()) continue;
                field.setAccessible(true);
                Object nested = field.get(value);
                if (nested == null || nested == value) continue;
                Object found = findNativeJavaMethod(nested, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }
}
JAVA

probe_classes="$out/reflection-probe-classes"
mkdir -p "$probe_classes"
javac -encoding UTF-8 -proc:none -cp "$classpath" -d "$probe_classes" "$out/PinnedUpstreamAbiReflectionProbe.java"
set +e
java -Xmx1g -cp "$probe_classes:$classpath" PinnedUpstreamAbiReflectionProbe \
    > "$out/java-reflection-rhino-probe.txt" 2>&1
probe_status=$?
set -e
cat "$out/java-reflection-rhino-probe.txt"
if grep -q '^JAVA_REFLECTION_RESULT=PASS$' "$out/java-reflection-rhino-probe.txt"; then
    echo "JAVA_REFLECTION=PASS" > "$out/reflection-result.txt"
else
    echo "JAVA_REFLECTION=FAILED" > "$out/reflection-result.txt"
fi
echo "BADCAT_COMMIT=$expected_badcat" >> "$out/reflection-result.txt"
echo "LEGADO_COMMIT=$expected_legado" >> "$out/reflection-result.txt"
echo "TARGET_CLASS_COUNT=40" >> "$out/reflection-result.txt"
if (( probe_status != 0 )); then
    echo "RHINO_JAVAMEMBERS=FAILED_DIAGNOSTIC" >> "$out/reflection-result.txt"
    exit "$probe_status"
fi
grep -q '^RESULT=JAVA_REFLECTION_AND_RHINO_JAVAMEMBERS_PASS$' "$out/java-reflection-rhino-probe.txt"
echo "RHINO_JAVAMEMBERS=PASS" >> "$out/reflection-result.txt"
echo "APP_APK=NOT_RUN" >> "$out/reflection-result.txt"
echo "R8_DEX_DEVICE=NOT_RUN" >> "$out/reflection-result.txt"
cat "$out/reflection-result.txt"
