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
        target.setImageResource(android.R.drawable.ic_menu_gallery)
        if (rawUrl.isNullOrBlank()) return
        val url = runCatching { URL(rawUrl) }.getOrNull() ?: return
        if (url.protocol != "http" && url.protocol != "https") return
        target.tag = rawUrl
        val reference = WeakReference(target)
        worker.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "image/*")
                }
                connection.connect()
                if (connection.responseCode !in 200..299 || connection.contentLength > 2 * 1024 * 1024) return@execute
                val bytes = connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 2 * 1024 * 1024) return@execute
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth > 8_000 || bounds.outHeight > 8_000) return@execute
                val sample = maxOf(1, Integer.highestOneBit(maxOf(bounds.outWidth / 720, bounds.outHeight / 960)))
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@execute
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val imageView = reference.get()
                    if (imageView != null && imageView.tag == rawUrl) imageView.setImageBitmap(bitmap)
                }
            } catch (_: Exception) {
                // Leave the placeholder visible.
            } finally {
                connection?.disconnect()
            }
        }
    }
}
