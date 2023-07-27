package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowErrorDetailRes(
    @SerializedName("loc")
    val loc: List<String>?,

    @SerializedName("msg")
    val msg: String?,

    @SerializedName("type")
    val type: String?,
)