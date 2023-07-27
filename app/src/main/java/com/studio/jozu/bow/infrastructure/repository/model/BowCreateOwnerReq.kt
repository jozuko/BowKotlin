package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowCreateOwnerReq(
    @SerializedName("email")
    val mailAddress: String
)