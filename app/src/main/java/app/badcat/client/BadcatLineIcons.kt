package app.badcat.client

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/**
 * A consistent 24dp line-icon family for the bottom bar and personal tools.
 * All glyphs share the same optical box, stroke, cap and join.
 */
internal class BadcatLineIconView(
    context: Context,
    private val key: String,
    tint: Int,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tint
        style = Paint.Style.STROKE
        strokeWidth = 1.85f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val save = canvas.save()
        val side = minOf(width, height).toFloat()
        canvas.translate((width - side) / 2f, (height - side) / 2f)
        canvas.scale(side / 24f, side / 24f)
        when (key) {
            "search" -> drawSearch(canvas)
            "favorites" -> drawFavorites(canvas)
            "mine" -> drawPerson(canvas)
            "messages" -> drawMessages(canvas)
            "history" -> drawHistory(canvas)
            "bookmarks" -> drawBookmark(canvas)
            "downloads" -> drawDownload(canvas)
            "feedback" -> drawFeedback(canvas)
            "sync" -> drawSync(canvas)
            "share" -> drawShare(canvas)
            "sources" -> drawBooks(canvas)
            else -> drawPerson(canvas)
        }
        canvas.restoreToCount(save)
    }

    private fun drawSearch(c: Canvas) {
        c.drawCircle(10.5f, 10.5f, 6.2f, paint)
        line(c, 15.2f, 15.2f, 20.2f, 20.2f)
    }

    private fun drawFavorites(c: Canvas) {
        path.reset()
        path.moveTo(12f, 20f)
        path.cubicTo(10.5f, 18.8f, 3.5f, 13.5f, 3.5f, 8.7f)
        path.cubicTo(3.5f, 3.2f, 10f, 2.3f, 12f, 7.2f)
        path.cubicTo(14f, 2.3f, 20.5f, 3.2f, 20.5f, 8.7f)
        path.cubicTo(20.5f, 13.5f, 13.5f, 18.8f, 12f, 20f)
        c.drawPath(path, paint)
    }

    private fun drawPerson(c: Canvas) {
        c.drawCircle(12f, 7.8f, 4f, paint)
        path.reset()
        path.moveTo(4f, 20.5f)
        path.cubicTo(4.5f, 15.8f, 7.3f, 13.5f, 12f, 13.5f)
        path.cubicTo(16.7f, 13.5f, 19.5f, 15.8f, 20f, 20.5f)
        c.drawPath(path, paint)
    }

    private fun drawMessages(c: Canvas) {
        rect.set(3.2f, 4f, 20.8f, 17f)
        c.drawRoundRect(rect, 3f, 3f, paint)
        path.reset()
        path.moveTo(8f, 17f); path.lineTo(6.2f, 20.5f); path.lineTo(12f, 17f)
        c.drawPath(path, paint)
        fillCircle(c, 8f, 10.5f, 0.8f)
        fillCircle(c, 12f, 10.5f, 0.8f)
        fillCircle(c, 16f, 10.5f, 0.8f)
    }

    private fun drawHistory(c: Canvas) {
        c.drawCircle(12f, 12f, 8.3f, paint)
        line(c, 12f, 7f, 12f, 12f)
        line(c, 12f, 12f, 16f, 14.5f)
        path.reset()
        path.moveTo(4.4f, 7f); path.lineTo(4.2f, 3.8f); path.lineTo(7.4f, 4.2f)
        c.drawPath(path, paint)
    }

    private fun drawBookmark(c: Canvas) {
        path.reset()
        path.moveTo(6f, 3.5f); path.lineTo(18f, 3.5f); path.lineTo(18f, 20.5f)
        path.lineTo(12f, 16.5f); path.lineTo(6f, 20.5f); path.close()
        c.drawPath(path, paint)
    }

    private fun drawDownload(c: Canvas) {
        path.reset()
        path.moveTo(5f, 15.5f); path.lineTo(5f, 19f); path.lineTo(19f, 19f); path.lineTo(19f, 15.5f)
        c.drawPath(path, paint)
        line(c, 12f, 3.5f, 12f, 14f)
        path.reset()
        path.moveTo(8f, 10.5f); path.lineTo(12f, 14.5f); path.lineTo(16f, 10.5f)
        c.drawPath(path, paint)
    }

    private fun drawFeedback(c: Canvas) {
        rect.set(3.2f, 3.5f, 20.8f, 17.2f)
        c.drawRoundRect(rect, 3f, 3f, paint)
        path.reset()
        path.moveTo(8f, 17.2f); path.lineTo(6f, 20.5f); path.lineTo(12f, 17.2f)
        c.drawPath(path, paint)
        line(c, 12f, 7f, 12f, 11.5f)
        fillCircle(c, 12f, 14.2f, 0.75f)
    }

    private fun drawSync(c: Canvas) {
        path.reset()
        path.addArc(RectF(4f, 4f, 20f, 20f), 205f, 145f)
        c.drawPath(path, paint)
        path.reset()
        path.moveTo(4.2f, 8.2f); path.lineTo(4.2f, 4.5f); path.lineTo(7.8f, 4.8f)
        c.drawPath(path, paint)
        path.reset()
        path.addArc(RectF(4f, 4f, 20f, 20f), 25f, 145f)
        c.drawPath(path, paint)
        path.reset()
        path.moveTo(19.8f, 15.8f); path.lineTo(19.8f, 19.5f); path.lineTo(16.2f, 19.2f)
        c.drawPath(path, paint)
    }

    private fun drawShare(c: Canvas) {
        line(c, 7f, 12f, 17f, 6.5f)
        line(c, 7f, 12f, 17f, 17.5f)
        c.drawCircle(5.5f, 12f, 2.4f, paint)
        c.drawCircle(18.5f, 5.5f, 2.4f, paint)
        c.drawCircle(18.5f, 18.5f, 2.4f, paint)
    }

    private fun drawBooks(c: Canvas) {
        rect.set(3.5f, 4f, 10.5f, 19.5f); c.drawRoundRect(rect, 1.2f, 1.2f, paint)
        rect.set(13.5f, 4f, 20.5f, 19.5f); c.drawRoundRect(rect, 1.2f, 1.2f, paint)
        line(c, 7f, 8f, 7f, 15f); line(c, 17f, 8f, 17f, 15f)
        line(c, 10.5f, 6f, 13.5f, 6f)
        line(c, 10.5f, 18f, 13.5f, 18f)
    }

    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) =
        c.drawLine(x1, y1, x2, y2, paint)

    private fun fillCircle(c: Canvas, x: Float, y: Float, radius: Float) {
        val old = paint.style
        paint.style = Paint.Style.FILL
        c.drawCircle(x, y, radius, paint)
        paint.style = old
    }
}
