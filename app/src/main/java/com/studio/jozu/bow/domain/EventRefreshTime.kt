package com.studio.jozu.bow.domain

import com.google.gson.annotations.SerializedName

data class EventRefreshTime(
    @SerializedName("month")
    val month: Int,

    @SerializedName("updated_at")
    val updatedAt: Int,
)