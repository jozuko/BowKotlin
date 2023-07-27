package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowDogRes(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("order")
    val order: Int?,
    @SerializedName("birth")
    val birth: Int?,
    @SerializedName("gender")
    val genderNo: Int?,
    @SerializedName("color")
    val colorNo: Int?,
    @SerializedName("image_path")
    val imagePath: String?,
    @SerializedName("enabled")
    val enabled: Boolean?,
    @SerializedName("updated_at")
    val updatedAt: Int?,
)