package app.badcat.engine

import java.lang.reflect.Type
import kotlinx.coroutines.Job
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RuntimeHostPortContractTest {

    private fun context(
        caller: SourceCallerKind = SourceCallerKind.SOURCE_SCRIPT,
        navigation: SourceNavigationPolicy = SourceNavigationPolicy.ALLOW,
        job: Job = Job(),
    ) = SourceExecutionContext(
        source = SourceIdentity("source-1", "Fixture source", 0),
        caller = caller,
        operationId = "operation-1",
        navigationPolicy = navigation,
        coroutineJob = job,
    )

    @Test
    fun sourceIdentityAndCallerContextAreExplicitAndStable() {
        val requestContext = context(caller = SourceCallerKind.LOGIN_EXTENSION)
        assertEquals("source-1", requestContext.source.sourceId)
        assertEquals("Fixture source", requestContext.source.sourceName)
        assertEquals(SourceCallerKind.LOGIN_EXTENSION, requestContext.caller)
        assertEquals("operation-1", requestContext.operationId)
        assertEquals(SourceNavigationPolicy.ALLOW, requestContext.navigationPolicy)
    }

    @Test
    fun hiddenWebViewCaptureRequiresRegexAndPreservesCacheAndDelay() {
        assertFailsWith<IllegalArgumentException> {
            HiddenWebViewRequest(
                context = context(),
                operation = HiddenWebViewOperation.CAPTURE_RESOURCE_URL,
                url = "https://reader.example/",
                html = null,
                javascript = "return document.title",
                timeoutMillis = 5_000,
            )
        }

        val request = HiddenWebViewRequest(
            context = context(),
            operation = HiddenWebViewOperation.CAPTURE_RESOURCE_URL,
            url = "https://reader.example/",
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
    fun hiddenWebViewResultsPreserveNullableValuesAndDistinctCaptureKinds() {
        assertEquals(HiddenWebViewResult.PageValue(null), HiddenWebViewResult.PageValue(null))
        assertIs<HiddenWebViewResult.ResourceUrl>(HiddenWebViewResult.ResourceUrl(null))
        assertIs<HiddenWebViewResult.OverrideUrl>(HiddenWebViewResult.OverrideUrl("https://reader.example/final"))
    }

    @Test
    fun browserVerificationPreservesFinalUrlAndDeliveryMode() {
        val captured = BrowserVerificationResult(
            "https://reader.example/final",
            "<html>captured</html>",
            BrowserBodyDelivery.CAPTURED_FROM_BROWSER,
        )
        val refetched = captured.copy(delivery = BrowserBodyDelivery.REFETCHED_AFTER_VERIFICATION)
        assertEquals(captured.finalUrl, refetched.finalUrl)
        assertEquals(BrowserBodyDelivery.REFETCHED_AFTER_VERIFICATION, refetched.delivery)
    }

    @Test
    fun verificationCodeRequestIsDistinctAndBounded() {
        val request = VerificationCodeRequest(context(), "https://reader.example/code.png", 30_000)
        assertEquals("https://reader.example/code.png", request.imageUrl)
        assertEquals(30_000L, request.timeoutMillis)
        assertFailsWith<IllegalArgumentException> {
            VerificationCodeRequest(context(), " ", 30_000)
        }
    }

    @Test
    fun coroutineCancellationParentIsNotReplacedByUiOutcome() {
        val job = Job()
        val execution = context(job = job)
        assertTrue(execution.coroutineJob.isActive)
        job.cancel()
        assertFalse(execution.coroutineJob.isActive)
        assertEquals(
            HostPortResult.Cancelled(HostCancellationReason.USER_DISMISSED),
            HostPortResult.Cancelled(HostCancellationReason.USER_DISMISSED),
        )
    }

    @Test
    fun uiDispatchOutcomesDistinguishAcceptedBlockedUnavailableAndFailed() {
        val outcomes = listOf(
            UiDispatchOutcome.Accepted("request-1"),
            UiDispatchOutcome.Blocked(NavigationBlockReason.SOURCE_POLICY),
            UiDispatchOutcome.Unavailable(UiUnavailableReason.ACTIVITY_STATE_SAVED),
            UiDispatchOutcome.Failed(HostPortError(HostPortErrorCode.EXECUTION_FAILED, "dispatch failed")),
        )
        assertIs<UiDispatchOutcome.Accepted>(outcomes[0])
        assertIs<UiDispatchOutcome.Blocked>(outcomes[1])
        assertIs<UiDispatchOutcome.Unavailable>(outcomes[2])
        assertIs<UiDispatchOutcome.Failed>(outcomes[3])
    }

    @Test
    fun sslCertificateFailuresAreTypedBoundedAndDistinctFromCancellation() {
        val details = SslCertificateFailure(
            host = "reader.example",
            category = SslCertificateErrorCategory.UNTRUSTED,
        )
        val result = HostPortResult.Failure(
            HostPortError(
                code = HostPortErrorCode.SSL_CERTIFICATE,
                message = "Source WebView certificate validation failed",
                sslFailure = details,
            )
        )
        val failure = assertIs<HostPortResult.Failure>(result)
        assertEquals(HostPortErrorCode.SSL_CERTIFICATE, failure.error.code)
        assertEquals(details, failure.error.sslFailure)
        assertIs<HostPortResult.Cancelled>(HostPortResult.Cancelled(HostCancellationReason.HOST_SHUTDOWN))

        assertFailsWith<IllegalArgumentException> {
            SslCertificateFailure(" ", SslCertificateErrorCategory.OTHER)
        }
        assertFailsWith<IllegalArgumentException> {
            SslCertificateFailure("reader.example/path", SslCertificateErrorCategory.OTHER)
        }
        assertFailsWith<IllegalArgumentException> {
            SslCertificateFailure("x".repeat(SslCertificateFailure.MAX_HOST_CHARS + 1), SslCertificateErrorCategory.OTHER)
        }
        assertFailsWith<IllegalArgumentException> {
            HostPortError(HostPortErrorCode.SSL_CERTIFICATE, "missing typed SSL metadata")
        }
        assertFailsWith<IllegalArgumentException> {
            HostPortError(
                HostPortErrorCode.EXECUTION_FAILED,
                "unexpected SSL metadata",
                sslFailure = details,
            )
        }
    }

    @Test
    fun productRefreshEventsRemainIndependent() {
        assertEquals(
            listOf("BOOK_INFO", "BOOK_TOC", "CURRENT_CONTENT"),
            ProductRefreshEvent.entries.map { it.name },
        )
    }

    @Test
    fun publicHostPortSurfaceHasNoAndroidOrViewTypes() {
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
            ProductRefreshEvent::class.java,
            HiddenWebViewPort::class.java,
            VisibleBrowserPort::class.java,
            VerificationCodePort::class.java,
            HostUiPort::class.java,
            ProductEventPort::class.java,
        )
        val exposedTypes = buildList<Type> {
            apiTypes.forEach { type ->
                type.genericSuperclass?.let(::add)
                addAll(type.genericInterfaces)
                type.declaredFields.forEach { add(it.genericType) }
                type.declaredConstructors.forEach { constructor ->
                    addAll(constructor.genericParameterTypes)
                }
                type.declaredMethods.forEach { method ->
                    add(method.genericReturnType)
                    addAll(method.genericParameterTypes)
                    addAll(method.genericExceptionTypes)
                }
            }
        }
        assertTrue(exposedTypes.none { it.typeName.contains("android.") || it.typeName.contains("androidx.") })
        assertNull(SourceExecutionContext::class.java.declaredFields.firstOrNull { it.type.name.contains("android.") })
    }
}
