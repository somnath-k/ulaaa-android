package com.dotkios.ulaaa.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest

/**
 * Builders for map markers styled like rounded photo bubbles: a white rounded
 * card with the photo (or a coloured initial) inside and a little pointer at the
 * bottom, matching the modern social-map look.
 */
object MapMarkers {

    private const val SIZE = 150       // card side, px
    private const val CORNER = 42f     // corner radius, px
    private const val BORDER = 10f     // white border, px
    private const val POINTER = 26f    // bottom pointer height, px

    /** Loads [imageUrl] and renders it into a photo bubble; null if the image can't load. */
    suspend fun photoBubble(context: Context, imageUrl: String?): Bitmap? {
        val photo = imageUrl?.let { loadBitmap(context, it) } ?: return null
        return bubble(context) { canvas, rect, paint ->
            val shader = BitmapShader(
                centerCrop(photo, rect.width().toInt(), rect.height().toInt()),
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP,
            )
            paint.shader = shader
            canvas.translate(rect.left, rect.top)
            canvas.drawRoundRect(
                RectF(0f, 0f, rect.width(), rect.height()),
                CORNER - BORDER, CORNER - BORDER, paint,
            )
            canvas.translate(-rect.left, -rect.top)
            paint.shader = null
        }
    }

    /** A coloured bubble with a single [initial] — used for friends / the current user. */
    fun initialBubble(context: Context, initial: String, color: Int): Bitmap =
        bubble(context) { canvas, rect, paint ->
            paint.color = color
            canvas.drawRoundRect(rect, CORNER - BORDER, CORNER - BORDER, paint)
            paint.color = Color.WHITE
            paint.textSize = rect.height() * 0.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            val cx = rect.centerX()
            val cy = rect.centerY() - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(initial.take(1).uppercase(), cx, cy, paint)
        }

    private inline fun bubble(
        context: Context,
        drawInner: (canvas: Canvas, innerRect: RectF, paint: Paint) -> Unit,
    ): Bitmap {
        val w = SIZE
        val h = (SIZE + POINTER).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Drop shadow + white card
        paint.color = Color.WHITE
        paint.setShadowLayer(9f, 0f, 4f, 0x40000000)
        val card = RectF(6f, 6f, w - 6f, SIZE - 6f)
        canvas.drawRoundRect(card, CORNER, CORNER, paint)
        // Pointer
        val path = Path().apply {
            moveTo(w / 2f - 16f, SIZE - 8f)
            lineTo(w / 2f + 16f, SIZE - 8f)
            lineTo(w / 2f, SIZE + POINTER - 8f)
            close()
        }
        canvas.drawPath(path, paint)
        paint.clearShadowLayer()

        val inner = RectF(card.left + BORDER, card.top + BORDER, card.right - BORDER, card.bottom - BORDER)
        drawInner(canvas, inner, paint)
        return bitmap
    }

    private fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        if (w <= 0 || h <= 0) return src
        val scale = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
        val sw = (w / scale).toInt().coerceIn(1, src.width)
        val sh = (h / scale).toInt().coerceIn(1, src.height)
        val x = (src.width - sw) / 2
        val y = (src.height - sh) / 2
        return Bitmap.createScaledBitmap(Bitmap.createBitmap(src, x, y, sw, sh), w, h, true)
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // must be software to draw onto a Canvas
            .build()
        val drawable = context.imageLoader.execute(request).drawable
        return (drawable as? BitmapDrawable)?.bitmap
    }
}
