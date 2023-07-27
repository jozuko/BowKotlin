package com.studio.jozu.bow.domain

import androidx.annotation.DrawableRes

data class MenuItem(
    @DrawableRes val imageRes: Int,
    val title: String,
)