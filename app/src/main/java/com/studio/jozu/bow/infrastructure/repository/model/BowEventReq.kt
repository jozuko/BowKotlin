package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowEventReq(
    @SerializedName("dog_id")
    val dogId: String?,
    @SerializedName("task_id")
    val taskId: String?,
    @SerializedName("timestamp")
    val timestamp: Int?,
)
