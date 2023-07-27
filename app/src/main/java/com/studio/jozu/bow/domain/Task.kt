package com.studio.jozu.bow.domain

import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.domain.extension.CalendarEx.unixTime
import com.studio.jozu.bow.infrastructure.realm.model.PersistTask
import com.studio.jozu.bow.infrastructure.repository.model.BowTaskReq
import com.studio.jozu.bow.infrastructure.repository.model.BowTaskRes
import java.util.*

data class Task(
    val persistId: String?,
    val taskId: String,
    val title: String,
    val icon: TaskIcon,
    var enabled: Boolean,
    val order: Int,
    var updatedAt: Calendar,
) {
    companion object {
        val defaultTaskList = listOf(
            Task(
                persistId = null,
                taskId = "",
                title = "ごはん",
                icon = TaskIcon.FOOD,
                enabled = true,
                order = 1,
                updatedAt = CalendarEx.now,
            )
        )

        val emptyTask: Task
            get() = Task(
                persistId = null,
                taskId = "",
                title = "",
                icon = TaskIcon.FOOD,
                enabled = true,
                order = 999,
                updatedAt = CalendarEx.now,
            )
    }

    class Builder {
        fun build(persistTask: PersistTask): Task {
            return Task(
                persistId = persistTask.recordId,
                taskId = persistTask.taskId,
                title = persistTask.title,
                icon = TaskIcon.getType(persistTask.iconNo),
                enabled = persistTask.enabled,
                order = persistTask.order,
                updatedAt = CalendarEx.fromUnixTime(persistTask.updatedAt),
            )
        }

        fun build(apiTask: BowTaskRes?): Task? {
            apiTask ?: return null
            val id = apiTask.id ?: return null
            val title = apiTask.title ?: return null
            val icon = TaskIcon.getType(apiTask.iconNo ?: -1)
            val enabled = apiTask.enabled ?: false
            val order = apiTask.order ?: 999
            val updatedAt = CalendarEx.fromUnixTime(apiTask.updatedAt)

            return Task(
                persistId = null,
                taskId = id,
                title = title,
                icon = icon,
                enabled = enabled,
                order = order,
                updatedAt = updatedAt,
            )
        }
    }

    val toRequestApiModel: BowTaskReq
        get() = BowTaskReq(
            title = title,
            iconNo = icon.iconNo,
            order = order,
            enabled = enabled,
        )

    val toPersistTask: PersistTask
        get() = PersistTask(
            recordId = persistId ?: UUID.randomUUID().toString(),
            title = title,
            iconNo = icon.iconNo,
            order = order,
            enabled = enabled,
            taskId = taskId,
            updatedAt = updatedAt.unixTime,
        )
}