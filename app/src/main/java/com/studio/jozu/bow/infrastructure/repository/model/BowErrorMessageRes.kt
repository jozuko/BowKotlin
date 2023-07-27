package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowErrorMessageRes(
    @SerializedName("detail")
    val detail: String?
)