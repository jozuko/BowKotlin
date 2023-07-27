package com.studio.jozu.bow.presentation.main.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.studio.jozu.bow.R

class CalendarEventBackground @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        setWillNotDraw(false)
    }

    @ColorRes
    var fillColor: Int = R.color.white
        set(value) {
            field = value
            backgroundPaint.color = ContextCompat.getColor(context, value)
        }

    private val rectF: RectF = RectF(0f, 0f, 0f, 0f)

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas?) {
        val radius = height.toFloat() / 4
        rectF.bottom = height.toFloat()
        rectF.right = width.toFloat()

        canvas?.drawRoundRect(rectF, radius, radius, backgroundPaint)
    }
}