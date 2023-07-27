package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowErrorRes(
    @SerializedName("detail")
    val detail: List<BowErrorDetailRes>?
)

