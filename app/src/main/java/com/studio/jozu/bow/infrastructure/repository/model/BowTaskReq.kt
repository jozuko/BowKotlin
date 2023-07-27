package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowTaskReq(
    @SerializedName("icon_no")
    val iconNo: Int?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("order")
    val order: Int?,
    @SerializedName("enabled")
    val enabled: Boolean?,
)
