package com.studio.jozu.bow.infrastructure.repository.model

import com.google.gson.annotations.SerializedName

data class BowEventRes(
    @SerializedName("id")
    val id: String?,
    @SerializedName("dog_id")
    val dogId: String?,
    @SerializedName("task_id")
    val taskId: String?,
    @SerializedName("timestamp")
    val timestamp: Int?,
    @SerializedName("updated_at")
    val updatedAt: Int?,
)
