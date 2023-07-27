package com.studio.jozu.bow.presentation.picasso

import android.graphics.*
import com.squareup.picasso.Transformation
import kotlin.math.min

class PicassoRoundedSquareTransform(
    private val size: Int,
    private val radius: Float,
) : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val sourceSize = min(source.width, source.height)
        val sourceCenterX = (source.width - sourceSize) / 2
        val sourceCenterY = (source.height - sourceSize) / 2

        val squaredBitmap = Bitmap.createBitmap(source, sourceCenterX, sourceCenterY, sourceSize, sourceSize)
        if (squaredBitmap != source) {
            source.recycle()
        }

        val scaledBitmap = Bitmap.createScaledBitmap(squaredBitmap, size, size, false)
        if (squaredBitmap != scaledBitmap) {
            squaredBitmap.recycle()
        }

        val bitmap = Bitmap.createBitmap(size, size, source.config)

        val canvas = Canvas(bitmap)
        val paint = Paint()
        val shader = BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.isAntiAlias = true

        canvas.drawRoundRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), radius, radius, paint)

        scaledBitmap.recycle()
        return bitmap
    }

    override fun key(): String {
        return PicassoRoundedSquareTransform::class.java.name
    }
}