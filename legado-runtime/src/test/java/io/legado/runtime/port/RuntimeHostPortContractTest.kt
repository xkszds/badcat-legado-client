package io.legado.runtime.port

import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
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
}
