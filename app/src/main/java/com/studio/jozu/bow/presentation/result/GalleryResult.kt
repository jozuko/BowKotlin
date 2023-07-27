package com.studio.jozu.bow.presentation.result

import android.net.Uri

data class GalleryResult(
    val isCanceled: Boolean,
    val uri: Uri?,
)