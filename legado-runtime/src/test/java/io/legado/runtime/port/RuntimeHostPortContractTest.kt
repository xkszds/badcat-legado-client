package io.legado.runtime.port

import java.lang.reflect.Type
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeHostPortContractTest {
    @Test
    fun sourceIdentityAndExecutionContextRequireStableIdentifiers() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceIdentity(sourceId = " ", sourceName = "Book source", sourceType = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SourceIdentity(sourceId = "https://source.example", sourceName = " ", sourceType = 0)
        }

        val identity = SourceIdentity(
            sourceId = "https://source.example",
            sourceName = "Book source",
            sourceType = 0,
        )
        assertThrows(IllegalArgumentException::class.java) {
            SourceExecutionContext(
                source = identity,
                caller = SourceCallerKind.SOURCE_SCRIPT,
                operationId = " ",
                navigationPolicy = SourceNavigationPolicy.BLOCK,
                coroutineJob = Job(),
            )
        }

        val job = Job()
        val context = SourceExecutionContext(
            source = identity,
            caller = SourceCallerKind.LOGIN_EXTENSION,
            operationId = "login-1",
            navigationPolicy = SourceNavigationPolicy.BLOCK,
            coroutineJob = job,
        )
        assertEquals(identity, context.source)
        assertEquals(SourceCallerKind.LOGIN_EXTENSION, context.caller)
        assertEquals("login-1", context.operationId)
        assertEquals(SourceNavigationPolicy.BLOCK, context.navigationPolicy)
        assertTrue(context.coroutineJob === job)
    }

    @Test
    fun sslCertificateErrorsRequireTypedCertificateMetadata() {
        assertThrows(IllegalArgumentException::class.java) {
            HostPortError(code = HostPortErrorCode.SSL_CERTIFICATE, message = "TLS failed")
        }
        assertThrows(IllegalArgumentException::class.java) {
            HostPortError(
                code = HostPortErrorCode.EXECUTION_FAILED,
                message = "Request failed",
                sslFailure = SslCertificateFailure("source.example", SslCertificateErrorCategory.UNTRUSTED),
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            SslCertificateFailure("https://source.example/path", SslCertificateErrorCategory.UNTRUSTED)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SslCertificateFailure("source.example/path", SslCertificateErrorCategory.UNTRUSTED)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SslCertificateFailure(
                "x".repeat(SslCertificateFailure.MAX_HOST_CHARS + 1),
                SslCertificateErrorCategory.OTHER,
            )
        }
        val ssl = SslCertificateFailure("source.example", SslCertificateErrorCategory.UNTRUSTED)
        assertEquals(HostPortErrorCode.SSL_CERTIFICATE,
            HostPortError(HostPortErrorCode.SSL_CERTIFICATE, "TLS failed", sslFailure = ssl).code)
    }

    @Test
    fun hostPortResultPreservesUserCancellationAsDistinctOutcome() {
        val success: HostPortResult<String> = HostPortResult.Success("ok")
        val failure: HostPortResult<String> = HostPortResult.Failure(
            HostPortError(HostPortErrorCode.EXECUTION_FAILED, "host failed"),
        )
        val cancelled: HostPortResult<String> = HostPortResult.Cancelled(HostCancellationReason.USER_DISMISSED)

        assertEquals("ok", (success as HostPortResult.Success).value)
        assertEquals(HostPortErrorCode.EXECUTION_FAILED,
            (failure as HostPortResult.Failure).error.code)
        assertEquals(HostCancellationReason.USER_DISMISSED,
            (cancelled as HostPortResult.Cancelled).reason)
    }

    @Test
    fun hiddenWebViewUrlCaptureRequiresAnExplicitCapturePattern() {
        val context = SourceExecutionContext(
            source = SourceIdentity("https://source.example", "Book source", 0),
            caller = SourceCallerKind.SOURCE_SCRIPT,
            operationId = "capture-1",
            navigationPolicy = SourceNavigationPolicy.ALLOW,
            coroutineJob = Job(),
        )

        assertThrows(IllegalArgumentException::class.java) {
            HiddenWebViewRequest(
                context = context,
                operation = HiddenWebViewOperation.CAPTURE_RESOURCE_URL,
                url = "https://source.example/book",
                html = null,
                javascript = "void 0",
                captureRegex = null,
                timeoutMillis = 5_000,
            )
        }

        val request = HiddenWebViewRequest(
            context = context,
            operation = HiddenWebViewOperation.CAPTURE_RESOURCE_URL,
            url = "https://source.example/book",
            html = "<html/>",
            javascript = "return document.title",
            headers = mapOf("User-Agent" to "fixture"),
            cachePolicy = HiddenWebViewCachePolicy.CACHE_FIRST,
            captureRegex = "https://cdn\\.example/.*",
            delayMillis = 250,
            timeoutMillis = 5_000,
        )
        assertEquals(HiddenWebViewCachePolicy.CACHE_FIRST, request.cachePolicy)
        assertEquals(250L, request.delayMillis)
        assertEquals("fixture", request.headers["User-Agent"])
    }

    @Test
    fun hostUiActionsRejectBlankNavigationTargets() {
        val context = SourceExecutionContext(
            source = SourceIdentity("https://source.example", "Book source", 0),
            caller = SourceCallerKind.SOURCE_SCRIPT,
            operationId = "open-1",
            navigationPolicy = SourceNavigationPolicy.ALLOW,
            coroutineJob = Job(),
        )

        assertThrows(IllegalArgumentException::class.java) {
            HostUiAction.OpenUrl(context = context, url = " ")
        }
    }

    @Test
    fun hiddenWebViewResultsPreserveNullableValuesAndDistinctCaptureKinds() {
        assertEquals(HiddenWebViewResult.PageValue(null), HiddenWebViewResult.PageValue(null))
        assertTrue(HiddenWebViewResult.ResourceUrl(null) is HiddenWebViewResult.ResourceUrl)
        assertTrue(
            HiddenWebViewResult.OverrideUrl("https://source.example/final") is
                HiddenWebViewResult.OverrideUrl,
        )
    }

    @Test
    fun browserVerificationPreservesFinalUrlAndBodyDeliveryMode() {
        val captured = BrowserVerificationResult(
            finalUrl = "https://source.example/final",
            body = "<html>captured</html>",
            delivery = BrowserBodyDelivery.CAPTURED_FROM_BROWSER,
        )
        val refetched = captured.copy(delivery = BrowserBodyDelivery.REFETCHED_AFTER_VERIFICATION)

        assertEquals(captured.finalUrl, refetched.finalUrl)
        assertEquals(captured.body, refetched.body)
        assertEquals(BrowserBodyDelivery.REFETCHED_AFTER_VERIFICATION, refetched.delivery)
    }

    @Test
    fun verificationCodeRequestIsDistinctAndRequiresBoundedInput() {
        val context = SourceExecutionContext(
            source = SourceIdentity("https://source.example", "Book source", 0),
            caller = SourceCallerKind.SOURCE_SCRIPT,
            operationId = "verification-1",
            navigationPolicy = SourceNavigationPolicy.ALLOW,
            coroutineJob = Job(),
        )
        val request = VerificationCodeRequest(context, "https://source.example/code.png", 30_000)
        assertEquals("https://source.example/code.png", request.imageUrl)
        assertEquals(30_000L, request.timeoutMillis)

        assertThrows(IllegalArgumentException::class.java) {
            VerificationCodeRequest(context, " ", 30_000)
        }
        assertThrows(IllegalArgumentException::class.java) {
            VerificationCodeRequest(context, "https://source.example/code.png", 0)
        }
    }

    @Test
    fun coroutineJobCancellationPropagatesThroughExecutionContext() {
        val job = Job()
        val context = SourceExecutionContext(
            source = SourceIdentity("https://source.example", "Book source", 0),
            caller = SourceCallerKind.SOURCE_SCRIPT,
            operationId = "cancel-1",
            navigationPolicy = SourceNavigationPolicy.ALLOW,
            coroutineJob = job,
        )

        assertTrue(context.coroutineJob?.isActive == true)
        job.cancel()
        assertFalse(context.coroutineJob?.isActive == true)
    }

    @Test
    fun uiDispatchOutcomesRemainDistinct() {
        val outcomes = listOf(
            UiDispatchOutcome.Accepted("request-1"),
            UiDispatchOutcome.Blocked(NavigationBlockReason.SOURCE_POLICY),
            UiDispatchOutcome.Unavailable(UiUnavailableReason.ACTIVITY_STATE_SAVED),
            UiDispatchOutcome.Failed(HostPortError(HostPortErrorCode.EXECUTION_FAILED, "dispatch failed")),
        )

        assertTrue(outcomes[0] is UiDispatchOutcome.Accepted)
        assertTrue(outcomes[1] is UiDispatchOutcome.Blocked)
        assertTrue(outcomes[2] is UiDispatchOutcome.Unavailable)
        assertTrue(outcomes[3] is UiDispatchOutcome.Failed)
    }

    @Test
    fun productRefreshEventsRemainIndependentAndStable() {
        assertEquals(
            listOf("BOOK_INFO", "BOOK_TOC", "CURRENT_CONTENT"),
            ProductRefreshEvent.values().map { it.name },
        )
    }

    @Test
    fun publicHostPortSurfaceDoesNotExposeAndroidTypes() {
        val apiTypes: List<Class<*>> = listOf(
            SourceIdentity::class.java,
            SourceExecutionContext::class.java,
            HostPortError::class.java,
            SslCertificateFailure::class.java,
            SslCertificateErrorCategory::class.java,
            HostPortResult::class.java,
            HiddenWebViewRequest::class.java,
            HiddenWebViewResult::class.java,
            BrowserVerificationRequest::class.java,
            BrowserVerificationResult::class.java,
            VerificationCodeRequest::class.java,
            UiDispatchOutcome::class.java,
            HostUiAction::class.java,
            HostUiAction.OpenUrl::class.java,
            HostUiAction.ShowBrowserDialog::class.java,
            HostUiAction.OpenVideoPlayer::class.java,
            ProductRefreshEvent::class.java,
            HiddenWebViewPort::class.java,
            VisibleBrowserPort::class.java,
            VerificationCodePort::class.java,
            HostUiPort::class.java,
            ProductEventPort::class.java,
        )
        val exposedTypes = mutableListOf<Type>()
        apiTypes.forEach { type ->
            type.genericSuperclass?.let(exposedTypes::add)
            exposedTypes.addAll(type.genericInterfaces)
            type.declaredFields.forEach { exposedTypes.add(it.genericType) }
            type.declaredConstructors.forEach { exposedTypes.addAll(it.genericParameterTypes) }
            type.declaredMethods.forEach { method ->
                exposedTypes.add(method.genericReturnType)
                exposedTypes.addAll(method.genericParameterTypes)
                exposedTypes.addAll(method.genericExceptionTypes)
            }
        }

        assertTrue(exposedTypes.none {
            it.typeName.contains("android.") || it.typeName.contains("androidx.")
        })
        assertTrue(
            SourceExecutionContext::class.java.declaredFields.none {
                it.type.name.contains("android.") || it.type.name.contains("androidx.")
            },
        )
    }


    @Test
    fun sourceExecutionContextAllowsAnEmptyUpstreamCoroutineContext() {
        val context = SourceExecutionContext(
            source = SourceIdentity("https://source.example", "Book source", 0),
            caller = SourceCallerKind.SOURCE_SCRIPT,
            operationId = "empty-context",
            navigationPolicy = SourceNavigationPolicy.ALLOW,
        )

        assertEquals(null, context.coroutineJob)
    }

    @Test
    fun hiddenWebViewAllowsNullUrlWhenHtmlIsPresentAndUsesUpstreamDefaults() {
        val context = SourceExecutionContext(
            source = SourceIdentity("https://source.example", "Book source", 0),
            caller = SourceCallerKind.SOURCE_SCRIPT,
            operationId = "html-page",
            navigationPolicy = SourceNavigationPolicy.ALLOW,
        )
        val request = HiddenWebViewRequest(
            context = context,
            operation = HiddenWebViewOperation.EVALUATE_PAGE,
            url = null,
            html = "<html/>",
            javascript = null,
        )

        assertEquals(null, request.url)
        assertEquals("utf-8", request.effectiveEncoding)
        assertEquals(60_000L, request.timeoutMillis)
        assertEquals(900L, request.effectiveDelayMillis)
        assertEquals(HiddenWebViewRuleMode.PLAIN, request.ruleMode)
    }

    @Test
    fun hiddenWebViewRequiresUrlWhenHtmlIsAbsent() {
        val context = SourceExecutionContext(
            source = SourceIdentity("https://source.example", "Book source", 0),
            caller = SourceCallerKind.SOURCE_SCRIPT,
            operationId = "url-required",
            navigationPolicy = SourceNavigationPolicy.ALLOW,
        )

        assertThrows(IllegalArgumentException::class.java) {
            HiddenWebViewRequest(
                context = context,
                operation = HiddenWebViewOperation.EVALUATE_PAGE,
                url = null,
                html = null,
                javascript = "return document.title",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            HiddenWebViewRequest(
                context = context,
                operation = HiddenWebViewOperation.EVALUATE_PAGE,
                url = " ",
                html = "",
                javascript = "return document.title",
            )
        }
    }

    @Test
    fun hiddenWebViewKeepsNullJavascriptDistinctFromEmptyJavascript() {
        val context = SourceExecutionContext(
            source = SourceIdentity("https://source.example", "Book source", 0),
            caller = SourceCallerKind.SOURCE_SCRIPT,
            operationId = "js-null-empty",
            navigationPolicy = SourceNavigationPolicy.ALLOW,
        )
        val defaultScript = HiddenWebViewRequest(
            context = context,
            operation = HiddenWebViewOperation.EVALUATE_PAGE,
            url = "https://source.example",
            html = null,
            javascript = null,
        )
        val emptyScript = defaultScript.copy(javascript = "")

        assertEquals(null, defaultScript.javascript)
        assertEquals("", emptyScript.javascript)
        assertEquals(900L, defaultScript.effectiveDelayMillis)
        assertEquals(0L, emptyScript.effectiveDelayMillis)
    }

    @Test
    fun hiddenWebViewRuleModePreservesInitialResultAndEncodingOverride() {
        val context = SourceExecutionContext(
            source = SourceIdentity("https://source.example", "Book source", 0),
            caller = SourceCallerKind.SOURCE_SCRIPT,
            operationId = "webjs-rule",
            navigationPolicy = SourceNavigationPolicy.ALLOW,
        )
        val request = HiddenWebViewRequest(
            context = context,
            operation = HiddenWebViewOperation.EVALUATE_PAGE,
            url = null,
            html = "<html/>",
            javascript = "return window.result",
            encoding = "UTF-8",
            initialResultJson = "{\"page\":1}",
            ruleMode = HiddenWebViewRuleMode.WEBJS_RULE,
        )

        assertEquals("{\"page\":1}", request.initialResultJson)
        assertEquals(HiddenWebViewRuleMode.WEBJS_RULE, request.ruleMode)
        assertEquals("UTF-8", request.effectiveEncoding)
    }

}
