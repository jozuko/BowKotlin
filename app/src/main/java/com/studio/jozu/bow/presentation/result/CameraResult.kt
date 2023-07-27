package com.studio.jozu.bow.presentation.result

import android.graphics.Bitmap

data class CameraResult(
    val isCanceled: Boolean,
    val bitmap: Bitmap?,
)