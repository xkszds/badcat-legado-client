package app.badcat.client

import android.graphics.BitmapFactory
import android.widget.ImageView
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Small bounded loader for source-provided book covers. */
object BookCoverLoader {
    private val worker = Executors.newFixedThreadPool(3)

    fun load(target: ImageView, rawUrl: String?) {
        // Invalidate any in-flight request before validating the replacement URL.
        target.tag = null
        target.setImageResource(android.R.drawable.ic_menu_gallery)
        if (rawUrl.isNullOrBlank()) return
        val initialUrl = runCatching { URL(rawUrl) }.getOrNull() ?: return
        if (!CoverUrlPolicy.isSafe(initialUrl)) return
        target.tag = rawUrl
        val reference = WeakReference(target)
        worker.execute {
            var currentUrl = initialUrl
            var redirects = 0
            while (true) {
                val connection = try {
                    (currentUrl.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                        instanceFollowRedirects = false
                        setRequestProperty("Accept", "image/*")
                    }
                } catch (_: Exception) {
                    return@execute
                }
                try {
                    val status = connection.responseCode
                    if (status in REDIRECT_CODES) {
                        val location = connection.getHeaderField("Location") ?: return@execute
                        val next = CoverUrlPolicy.resolveRedirect(currentUrl, location) ?: return@execute
                        if (redirects >= MAX_REDIRECTS) return@execute
                        redirects++
                        currentUrl = next
                        continue
                    }
                    if (status !in 200..299 || connection.contentLength > MAX_BYTES) return@execute
                    val bytes = connection.inputStream.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8 * 1024)
                        var total = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_BYTES) return@execute
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                        bounds.outWidth > MAX_DIMENSION || bounds.outHeight > MAX_DIMENSION
                    ) return@execute
                    val sample = maxOf(
                        1,
                        Integer.highestOneBit(maxOf(bounds.outWidth / 720, bounds.outHeight / 960)),
                    )
                    val bitmap = BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = sample },
                    ) ?: return@execute
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        val imageView = reference.get()
                        if (imageView != null && imageView.tag == rawUrl) imageView.setImageBitmap(bitmap)
                        else bitmap.recycle()
                    }
                    return@execute
                } catch (_: Exception) {
                    return@execute
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    private const val MAX_BYTES = 2 * 1024 * 1024
    private const val MAX_DIMENSION = 8_000
    private const val MAX_REDIRECTS = 5
    private val REDIRECT_CODES = setOf(300, 301, 302, 303, 307, 308)
}
