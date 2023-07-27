package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowDogReq(
    @SerializedName("name")
    val name: String,
    @SerializedName("order")
    val order: Int,
    @SerializedName("birth")
    val birth: Int,
    @SerializedName("gender")
    val genderNo: Int,
    @SerializedName("color")
    val colorNo: Int,
    @SerializedName("image_path")
    val imagePath: String?,
    @SerializedName("enabled")
    val enabled: Boolean,
)