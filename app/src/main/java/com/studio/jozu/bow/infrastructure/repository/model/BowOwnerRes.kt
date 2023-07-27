package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowOwnerRes(
    @SerializedName("id")
    val ownerId: String?,

    @SerializedName("email")
    val mailAddress: String?,
)