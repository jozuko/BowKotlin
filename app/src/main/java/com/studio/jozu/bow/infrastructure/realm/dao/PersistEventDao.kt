package com.studio.jozu.bow.infrastructure.realm.dao

import com.studio.jozu.bow.domain.Event
import com.studio.jozu.bow.domain.extension.CalendarEx.unixTime
import com.studio.jozu.bow.infrastructure.realm.RealmWrapper
import com.studio.jozu.bow.infrastructure.realm.model.PersistEvent
import java.util.*

class PersistEventDao(private val realmWrapper: RealmWrapper) {
    fun replace(events: List<Event>, from: Calendar, to: Calendar) {
        realmWrapper.getInstance().use { realm ->
            realm.executeTransaction { trans ->
                val persistEventList = events.map { event ->
                    val insertTarget = event.toPersistEvent
                    trans.where(PersistEvent::class.java)
                        .equalTo("id", event.eventId)
                        .findFirst()?.let { persistEvent ->
                            insertTarget.recordId = persistEvent.recordId
                        }
                    insertTarget
                }

                trans.where(PersistEvent::class.java)
                    .greaterThanOrEqualTo("timestamp", from.unixTime)
                    .lessThanOrEqualTo("timestamp", to.unixTime)
                    .findAll()
                    .deleteAllFromRealm()

                trans.insertOrUpdate(persistEventList)
            }
        }
    }

    fun find(from: Calendar, to: Calendar): List<Event> {
        realmWrapper.getInstance().use { realm ->
            return realm.where(PersistEvent::class.java)
                .greaterThanOrEqualTo("timestamp", from.unixTime)
                .lessThanOrEqualTo("timestamp", to.unixTime)
                .findAll()
                .mapNotNull { Event.Builder().builder(it) }
        }
    }

    fun save(event: Event) {
        realmWrapper.getInstance().use { realm ->
            realm.executeTransaction { trans ->
                val persistEvent = trans.where(PersistEvent::class.java)
                    .equalTo("id", event.eventId)
                    .findFirst() ?: event.toPersistEvent

                persistEvent.dogId = event.dogId
                persistEvent.taskId = event.taskId
                persistEvent.timestamp = event.timestamp.unixTime
                persistEvent.updatedAt = event.updatedAt.unixTime

                trans.insertOrUpdate(persistEvent)
            }
        }
    }

    fun delete(event: Event) {
        realmWrapper.getInstance().use { realm ->
            realm.executeTransaction { trans ->
                trans.where(PersistEvent::class.java)
                    .equalTo("id", event.eventId)
                    .findFirst()
                    ?.deleteFromRealm()
            }
        }
    }
}