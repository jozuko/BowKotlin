package com.studio.jozu.bow.domain.extension

import android.content.Context

object ContextEx {
    val Context.density: Float
        get() = this.resources.displayMetrics.density
}