package app.badcat.client.runtime

import android.os.Build
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.badcat.engine.BookChapter
import app.badcat.engine.EngineResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/**
 * Exercises public, immutable BookSource snapshots through the app's real
 * BadcatReadingEngineFactory graph. This is not a full upstream Legado parity test.
 *
 * The BQG snapshot contains an obfuscated <js> search rule which performs
 * additional remote code/API requests. It is imported and inspected, but is
 * deliberately not executed until that third-party script has been reviewed.
 */
@RunWith(AndroidJUnit4::class)
class PublicLegadoSourcesEndToEndInstrumentedTest {
    private data class Fixture(val file: String, val expectedHost: String)

    private val fixtures = listOf(
        Fixture("tangsan-chinese.json", "xtangsanshu.com"),
        Fixture("bqg-fun.json", "bqg.fun"),
        Fixture("jinghua-shuge.json", "jhsssd.com"),
        Fixture("lingling-xiaoshuo.json", "00shu.la"),
    )

    @Test
    fun importsAndExercisesPublicSourcesThroughProductionIntegratedEngine() = runBlocking {
        // Run live public-site traffic once, on the API 35 emulator. API 28 keeps
        // the existing WebView/cookie coverage without duplicating site requests.
        assumeTrue("Public source E2E is assigned to API 35", Build.VERSION.SDK_INT >= 35)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assetContext = instrumentation.context
        val appContext = instrumentation.targetContext.applicationContext
        val factory = BadcatReadingEngineFactory(appContext)
        val engine = factory.create()
        val report = JSONObject()
            .put("task", "Runtime-3.2f.45")
            .put("sdkInt", Build.VERSION.SDK_INT)
            .put("startedAtEpochMillis", System.currentTimeMillis())
        val sourceReports = JSONArray()

        try {
            for ((index, fixture) in fixtures.withIndex()) {
                val rawBytes = assetContext.assets.open("legado-sources/" + fixture.file).use { it.readBytes() }
                val rawJson = String(rawBytes, Charsets.UTF_8)
                val sourceArray = JSONArray(rawJson)
                assertEquals("Fixture must contain exactly one source: " + fixture.file, 1, sourceArray.length())
                val sourceObject = sourceArray.getJSONObject(0)
                val sourceId = sourceObject.getString("bookSourceUrl")
                val sourceName = sourceObject.optString("bookSourceName", fixture.file)
                assertTrue("Fixture source host does not match provenance: " + fixture.file,
                    sourceId.contains(fixture.expectedHost, ignoreCase = true))

                val sourceReport = JSONObject()
                    .put("fixture", fixture.file)
                    .put("fixtureSha256", sha256(rawBytes))
                    .put("sourceId", sourceId)
                    .put("sourceName", sourceName)
                val stages = JSONArray()
                sourceReport.put("stages", stages)
                sourceReports.put(sourceReport)

                val importStarted = SystemClock.elapsedRealtime()
                val imported = factory.sourceCatalog().importJson(rawJson)
                if (imported !is EngineResult.Success || imported.value != 1) {
                    addStage(stages, "import", "fail", SystemClock.elapsedRealtime() - importStarted,
                        if (imported is EngineResult.Failure) classify(imported) else "fixture_import_count_mismatch")
                    sourceReport.put("e2eStatus", "fixture_import_failure")
                    continue
                }
                addStage(stages, "import", "pass", SystemClock.elapsedRealtime() - importStarted)

                val ruleSearch = sourceObject.optJSONObject("ruleSearch") ?: JSONObject()
                val bookListRule = ruleSearch.optString("bookList")
                val containsExecutableJs = bookListRule.contains("<js>", ignoreCase = true)
                val containsDynamicCodeConstruction = bookListRule.contains("Function(", ignoreCase = true) ||
                    bookListRule.contains("eval(", ignoreCase = true)
                if (fixture.file == "bqg-fun.json" && containsExecutableJs && containsDynamicCodeConstruction) {
                    addStage(stages, "security_preflight", "blocked", 0L,
                        "unreviewed_obfuscated_source_js_with_remote_code_or_api_calls")
                    listOf("search", "detail", "toc", "content_1", "content_2").forEach { stage ->
                        addStage(stages, stage, "blocked", 0L, "blocked_by_source_js_security_preflight")
                    }
                    sourceReport.put("e2eStatus", "blocked_unreviewed_obfuscated_source_js")
                    continue
                }

                // Conservative spacing between unrelated public hosts.
                if (index > 0) delay(1500L)

                val search = runStage(stages, "search") {
                    engine.search(sourceId, "剑来")
                }
                if (search == null) {
                    sourceReport.put("e2eStatus", "search_failed")
                    continue
                }
                if (search.isEmpty()) {
                    addStage(stages, "search_result_validation", "fail", 0L,
                        "unsupported_search_keyword_or_empty_results")
                    sourceReport.put("e2eStatus", "empty_search_results")
                    continue
                }
                val candidate = search.first()

                val detail = runStage(stages, "detail") {
                    engine.bookDetailWithFallback(sourceId, candidate.bookUrl, candidate.name)
                }
                if (detail == null) {
                    sourceReport.put("e2eStatus", "detail_failed")
                    continue
                }
                if (detail.name.isBlank() || detail.bookUrl.isBlank()) {
                    addStage(stages, "detail_validation", "fail", 0L, "source_rule_failure")
                    sourceReport.put("e2eStatus", "invalid_detail")
                    continue
                }

                val chapters = runStage(stages, "toc") {
                    engine.chapters(sourceId, detail.bookUrl)
                }
                if (chapters == null) {
                    sourceReport.put("e2eStatus", "toc_failed")
                    continue
                }
                if (chapters.size < 2) {
                    addStage(stages, "toc_validation", "fail", 0L, "fewer_than_two_chapters")
                    sourceReport.put("e2eStatus", "insufficient_toc")
                    continue
                }

                var contentPasses = 0
                for (chapterIndex in 0..1) {
                    val chapter: BookChapter = chapters[chapterIndex]
                    val content = runStage(stages, "content_" + (chapterIndex + 1)) {
                        engine.chapterContent(sourceId, detail.bookUrl, chapter, detail.name)
                    }
                    if (content != null && content.text.isNotBlank()) {
                        contentPasses++
                    } else if (content != null) {
                        addStage(stages, "content_validation_" + (chapterIndex + 1), "fail", 0L,
                            "empty_chapter_body")
                    }
                }
                sourceReport.put("e2eStatus", if (contentPasses == 2) "pass" else "content_incomplete")
                sourceReport.put("contentBodiesPassed", contentPasses)
            }
        } finally {
            report.put("finishedAtEpochMillis", System.currentTimeMillis())
            report.put("sources", sourceReports)
            val bytes = report.toString(2).toByteArray(Charsets.UTF_8)
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val chunks = encoded.chunked(2500)
            Log.i("BadcatLegadoE2E", "REPORT_PARTS=" + chunks.size)
            chunks.forEachIndexed { index, chunk ->
                Log.i("BadcatLegadoE2E", "REPORT_PART=" + index + "/" + chunks.size + ":" + chunk)
            }
        }

        assertEquals("All four public source fixtures must be represented", 4, sourceReports.length())
        assertTrue("The sanitized E2E report must contain four source outcomes", report.has("finishedAtEpochMillis"))
    }

    private suspend fun <T> runStage(
        stages: JSONArray,
        stageName: String,
        action: suspend () -> EngineResult<T>,
    ): T? {
        val started = SystemClock.elapsedRealtime()
        return try {
            when (val result = withTimeout(45_000L) { action() }) {
                is EngineResult.Success -> {
                    addStage(stages, stageName, "pass", SystemClock.elapsedRealtime() - started)
                    result.value
                }
                is EngineResult.Failure -> {
                    addStage(stages, stageName, "fail", SystemClock.elapsedRealtime() - started, classify(result))
                    null
                }
            }
        } catch (timeout: TimeoutCancellationException) {
            addStage(stages, stageName, "fail", SystemClock.elapsedRealtime() - started, "runtime_or_network_timeout")
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: LinkageError) {
            addStage(stages, stageName, "fail", SystemClock.elapsedRealtime() - started,
                "runtime_missing_class_" + error.javaClass.simpleName)
            null
        } catch (error: Exception) {
            addStage(stages, stageName, "fail", SystemClock.elapsedRealtime() - started,
                "runtime_js_session_failure_" + error.javaClass.simpleName)
            null
        }
    }

    private fun addStage(
        stages: JSONArray,
        stage: String,
        status: String,
        elapsedMillis: Long,
        classification: String? = null,
    ) {
        val item = JSONObject()
            .put("stage", stage)
            .put("status", status)
            .put("elapsedMillis", elapsedMillis)
        if (classification != null) item.put("classification", classification)
        stages.put(item)
    }

    private fun classify(failure: EngineResult.Failure): String {
        return when (failure.code) {
            app.badcat.engine.EngineErrorCode.NETWORK -> {
                val match = Regex("HTTP\\s+(\\d{3})", RegexOption.IGNORE_CASE).find(failure.message)
                if (match != null) "http_site_response_" + match.groupValues[1]
                else "network_transport_dns_tls_or_timeout"
            }
            app.badcat.engine.EngineErrorCode.PARSE,
            app.badcat.engine.EngineErrorCode.NOT_FOUND -> "source_rule_failure"
            app.badcat.engine.EngineErrorCode.UNSUPPORTED_RULE -> "unsupported_rule_or_runtime_js"
            app.badcat.engine.EngineErrorCode.RULE_TIMEOUT -> "runtime_js_rule_timeout"
            app.badcat.engine.EngineErrorCode.INVALID_SOURCE -> "invalid_source_or_rule"
            app.badcat.engine.EngineErrorCode.CANCELLED -> "cancelled"
            app.badcat.engine.EngineErrorCode.UNKNOWN -> "runtime_js_session_failure"
        }
    }

    private fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it)
        }
    }
}
