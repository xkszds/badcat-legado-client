package io.legado.runtime.port

import kotlinx.coroutines.Job

/**
 * Platform-neutral contracts for services owned by the Android host.
 *
 * These are declarations only: no fake/default implementations are supplied.
 * Runtime callers must preserve [SourceExecutionContext] across asynchronous
 * callbacks. Implementations must not retain an Activity or View in this context.
 */
data class SourceIdentity(
    val sourceId: String,
    val sourceName: String,
    val sourceType: Int,
) {
    init {
        require(sourceId.isNotBlank()) { "sourceId must not be blank" }
        require(sourceName.isNotBlank()) { "sourceName must not be blank" }
    }
}

enum class SourceCallerKind {
    SOURCE_SCRIPT,
    LOGIN_EXTENSION,
    LEGACY_REVIEW_EXTENSION,
}

enum class SourceNavigationPolicy {
    ALLOW,
    BLOCK,
}

/**
 * Immutable execution snapshot. [coroutineJob] is the cancellation parent;
 * suspend implementations must propagate coroutine cancellation rather than
 * converting CancellationException into an ordinary host failure.
 */
data class SourceExecutionContext(
    val source: SourceIdentity,
    val caller: SourceCallerKind,
    val operationId: String,
    val navigationPolicy: SourceNavigationPolicy,
    val coroutineJob: Job,
) {
    init {
        require(operationId.isNotBlank()) { "operationId must not be blank" }
    }
}

enum class HostPortErrorCode {
    INVALID_REQUEST,
    TIMEOUT,
    ACTIVITY_UNAVAILABLE,
    ACTIVITY_STATE_SAVED,
    NAVIGATION_BLOCKED,
    EXECUTION_FAILED,
    RESOURCE_EXHAUSTED,
    INTERRUPTED,
    SSL_CERTIFICATE,
}

enum class SslCertificateErrorCategory {
    UNTRUSTED,
    EXPIRED,
    NOT_YET_VALID,
    HOSTNAME_MISMATCH,
    DATE_INVALID,
    INVALID,
    OTHER,
}

/** Safe, platform-neutral summary; never carries a certificate or Android WebView object. */
data class SslCertificateFailure(
    /** Normalized host only (no scheme, path, query, fragment, credentials or certificate data). */
    val host: String,
    val category: SslCertificateErrorCategory,
) {
    init {
        require(host.isNotBlank() && host.length <= MAX_HOST_CHARS) { "SSL failure host is invalid or oversized" }
        require(host == host.trim() && host.none { it.isWhitespace() || it.code < 0x20 || it.code == 0x7f }) {
            "SSL failure host must be a normalized host value"
        }
        require(!host.contains("://") && !host.contains('/') && !host.contains('?') &&
            !host.contains('#') && !host.contains('@')) {
            "SSL failure host must not contain URL components"
        }
    }

    companion object {
        const val MAX_HOST_CHARS = 253
    }
}

data class HostPortError(
    val code: HostPortErrorCode,
    val message: String,
    val cause: Throwable? = null,
    val sslFailure: SslCertificateFailure? = null,
) {
    init {
        require((code == HostPortErrorCode.SSL_CERTIFICATE) == (sslFailure != null)) {
            "SSL certificate failures require typed SSL metadata, and other errors must not carry it"
        }
    }
}

enum class HostCancellationReason {
    USER_DISMISSED,
    ACTIVITY_DESTROYED,
    HOST_SHUTDOWN,
}

/**
 * A user/host cancellation is a domain outcome. Coroutine Job cancellation is
 * separate and must still throw CancellationException from a suspended call.
 */
sealed interface HostPortResult<out T> {
    data class Success<T>(val value: T) : HostPortResult<T>
    data class Failure(val error: HostPortError) : HostPortResult<Nothing>
    data class Cancelled(val reason: HostCancellationReason) : HostPortResult<Nothing>
}

enum class HiddenWebViewOperation {
    EVALUATE_PAGE,
    CAPTURE_RESOURCE_URL,
    CAPTURE_OVERRIDE_URL,
}

enum class HiddenWebViewCachePolicy {
    BYPASS,
    CACHE_FIRST,
}

/**
 * One hidden/background WebView operation. Resource/override capture is not
 * equivalent to HTTP fetch and must be implemented by a real WebView owner.
 */
data class HiddenWebViewRequest(
    val context: SourceExecutionContext,
    val operation: HiddenWebViewOperation,
    val url: String,
    val html: String?,
    val javascript: String,
    val headers: Map<String, String> = emptyMap(),
    val cachePolicy: HiddenWebViewCachePolicy = HiddenWebViewCachePolicy.BYPASS,
    val captureRegex: String? = null,
    val delayMillis: Long = 0,
    val timeoutMillis: Long,
) {
    init {
        require(url.isNotBlank()) { "url must not be blank" }
        require(javascript.isNotBlank()) { "javascript must not be blank" }
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
        require(delayMillis >= 0) { "delayMillis must not be negative" }
        require(headers.keys.none(String::isBlank)) { "header names must not be blank" }
        if (operation != HiddenWebViewOperation.EVALUATE_PAGE) {
            require(!captureRegex.isNullOrBlank()) { "captureRegex is required for URL capture" }
        }
    }
}

sealed interface HiddenWebViewResult {
    data class PageValue(val value: String?) : HiddenWebViewResult
    data class ResourceUrl(val url: String?) : HiddenWebViewResult
    data class OverrideUrl(val url: String?) : HiddenWebViewResult
}

data class BrowserVerificationRequest(
    val context: SourceExecutionContext,
    val url: String,
    val title: String,
    val html: String? = null,
    val refetchAfterSuccess: Boolean = false,
    val timeoutMillis: Long,
) {
    init {
        require(url.isNotBlank()) { "url must not be blank" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
    }
}

enum class BrowserBodyDelivery {
    CAPTURED_FROM_BROWSER,
    REFETCHED_AFTER_VERIFICATION,
}

data class BrowserVerificationResult(
    val finalUrl: String,
    val body: String,
    val delivery: BrowserBodyDelivery,
) {
    init {
        require(finalUrl.isNotBlank()) { "finalUrl must not be blank" }
    }
}

data class VerificationCodeRequest(
    val context: SourceExecutionContext,
    val imageUrl: String,
    val timeoutMillis: Long,
) {
    init {
        require(imageUrl.isNotBlank()) { "imageUrl must not be blank" }
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
    }
}

enum class UiUnavailableReason {
    NO_TOP_ACTIVITY,
    ACTIVITY_FINISHING,
    ACTIVITY_DESTROYED,
    ACTIVITY_STATE_SAVED,
}

enum class NavigationBlockReason {
    SOURCE_POLICY,
    SOURCE_CONTEXT_CANCELLED,
}

sealed interface UiDispatchOutcome {
    data class Accepted(val requestId: String) : UiDispatchOutcome
    data class Blocked(val reason: NavigationBlockReason) : UiDispatchOutcome
    data class Unavailable(val reason: UiUnavailableReason) : UiDispatchOutcome
    data class Failed(val error: HostPortError) : UiDispatchOutcome
}

sealed interface HostUiAction {
    val context: SourceExecutionContext

    data class ShowBrowserDialog(
        override val context: SourceExecutionContext,
        val url: String,
        val html: String? = null,
        val preloadJavascript: String? = null,
        val configJson: String? = null,
    ) : HostUiAction {
        init {
            require(url.isNotBlank()) { "url must not be blank" }
        }
    }

    data class OpenUrl(
        override val context: SourceExecutionContext,
        val url: String,
        val mimeType: String? = null,
    ) : HostUiAction {
        init {
            require(url.isNotBlank()) { "url must not be blank" }
        }
    }

    data class OpenVideoPlayer(
        override val context: SourceExecutionContext,
        val url: String,
        val title: String,
        val floating: Boolean? = null,
    ) : HostUiAction {
        init {
            require(url.isNotBlank()) { "url must not be blank" }
            require(title.isNotBlank()) { "title must not be blank" }
        }
    }
}

enum class ProductRefreshEvent {
    BOOK_INFO,
    BOOK_TOC,
    CURRENT_CONTENT,
}

/** Hidden/background WebView ownership and pooling are Android-host concerns. */
interface HiddenWebViewPort {
    suspend fun execute(request: HiddenWebViewRequest): HostPortResult<HiddenWebViewResult>
}

/** Visible browser navigation and user-verification waiting remain separate. */
interface VisibleBrowserPort {
    suspend fun launch(request: BrowserVerificationRequest): UiDispatchOutcome
    suspend fun awaitVerification(request: BrowserVerificationRequest): HostPortResult<BrowserVerificationResult>
}

/** Image-code verification is not an alias for browser verification. */
interface VerificationCodePort {
    suspend fun awaitCode(request: VerificationCodeRequest): HostPortResult<String>
}

/** UI actions must report blocked/unavailable/failure instead of false success. */
interface HostUiPort {
    suspend fun dispatch(action: HostUiAction): UiDispatchOutcome
}

/** Product refresh signals remain distinct; adapters route them to Badcat pages. */
interface ProductEventPort {
    suspend fun publish(context: SourceExecutionContext, event: ProductRefreshEvent): UiDispatchOutcome
}
