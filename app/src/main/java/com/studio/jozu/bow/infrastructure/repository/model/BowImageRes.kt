package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowImageRes(
    @SerializedName("image_path")
    val imagePath: String?
)