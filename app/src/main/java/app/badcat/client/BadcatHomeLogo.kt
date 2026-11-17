package app.badcat.client

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.Gravity
import android.widget.ImageView

/**
 * Screenshot-backed Badcat 1.5.0.5 home wordmark.
 * The source image was extracted from the user-provided reference APK and
 * downsampled/cropped without changing its glyph artwork.
 */
internal object BadcatHomeLogo {
    private const val PNG_BASE64 =
        "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"

    fun create(context: Context): ImageView = ImageView(context).apply {
        val bytes = Base64.decode(PNG_BASE64, Base64.DEFAULT)
        setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = "坏坏猫"
        adjustViewBounds = true
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)
    }
}
