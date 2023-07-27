package com.studio.jozu.bow.domain

import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.domain.extension.CalendarEx.unixTime
import com.studio.jozu.bow.infrastructure.realm.model.PersistEvent
import com.studio.jozu.bow.infrastructure.repository.model.BowEventReq
import com.studio.jozu.bow.infrastructure.repository.model.BowEventRes
import java.util.*

data class Event(
    val persistId: String?,
    val eventId: String,
    val dogId: String,
    val taskId: String,
    val timestamp: Calendar,
    val updatedAt: Calendar,
) {
    class Builder {
        fun builder(persistEvent: PersistEvent): Event? {
            val timestamp = if (persistEvent.timestamp < 0) return null else CalendarEx.fromUnixTime(persistEvent.timestamp)
            val updatedAt = if (persistEvent.updatedAt < 0) return null else CalendarEx.fromUnixTime(persistEvent.updatedAt)

            return Event(
                persistId = persistEvent.recordId,
                eventId = persistEvent.id,
                dogId = persistEvent.dogId,
                taskId = persistEvent.taskId,
                timestamp = timestamp,
                updatedAt = updatedAt,
            )
        }

        fun builder(apiEvent: BowEventRes): Event? {
            return Event(
                persistId = null,
                eventId = apiEvent.id ?: return null,
                dogId = apiEvent.dogId ?: return null,
                taskId = apiEvent.taskId ?: return null,
                timestamp = apiEvent.timestamp?.let { CalendarEx.fromUnixTime(it) } ?: return null,
                updatedAt = apiEvent.updatedAt?.let { CalendarEx.fromUnixTime(it) } ?: return null,
            )
        }
    }

    val toPersistEvent: PersistEvent
        get() = PersistEvent(
            recordId = persistId ?: UUID.randomUUID().toString(),
            id = eventId,
            dogId = dogId,
            taskId = taskId,
            timestamp = timestamp.unixTime,
            updatedAt = updatedAt.unixTime,
        )

    val toRequestApiModel: BowEventReq
        get() = BowEventReq(
            dogId = dogId,
            taskId = taskId,
            timestamp = timestamp.unixTime,
        )

    fun getDog(dogList: List<Dog>): Dog? {
        return dogList.find { it.dogId == dogId }
    }

    fun getTask(taskList: List<Task>): Task? {
        return taskList.find { it.taskId == taskId }
    }
}