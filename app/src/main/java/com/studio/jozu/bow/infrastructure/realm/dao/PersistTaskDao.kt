package com.studio.jozu.bow.infrastructure.realm.dao

import com.studio.jozu.bow.domain.Task
import com.studio.jozu.bow.domain.extension.CalendarEx.unixTime
import com.studio.jozu.bow.infrastructure.realm.RealmWrapper
import com.studio.jozu.bow.infrastructure.realm.model.PersistTask
import io.realm.Sort
import timber.log.Timber

class PersistTaskDao(private val realmWrapper: RealmWrapper) {
    val count: Int
        get() {
            realmWrapper.getInstance().use { realm ->
                return realm.where(PersistTask::class.java)
                    .findAll()
                    .count()
            }
        }

    fun getAllTasks(): List<Task> {
        realmWrapper.getInstance().use { realm ->
            val persistList = realm.where(PersistTask::class.java)
                .findAll()
                .sort(arrayOf("order", "title", "updatedAt"), arrayOf(Sort.ASCENDING, Sort.ASCENDING, Sort.DESCENDING))
            return persistList.map { Task.Builder().build(it) }
        }
    }

    fun replaceData(tasks: List<Task>) {
        realmWrapper.getInstance().use { realm ->
            realm.executeTransaction { trans ->
                val persistList = tasks.map { it.toPersistTask }
                persistList.forEach { persistTask ->
                    trans.where(PersistTask::class.java)
                        .equalTo("taskId", persistTask.taskId)
                        .findFirst()?.let { findTask ->
                            persistTask.recordId = findTask.recordId
                        }
                }

                trans.delete(PersistTask::class.java)
                persistList.forEach {
                    trans.insertOrUpdate(it)
                }
            }
        }
    }

    fun saveByRecordId(task: Task) {
        realmWrapper.getInstance().use { realm ->
            realm.executeTransaction { trans ->
                val persistTask = trans.where(PersistTask::class.java)
                    .equalTo("recordId", task.persistId)
                    .findFirst()

                if (persistTask == null) {
                    trans.insertOrUpdate(task.toPersistTask)
                } else {
                    persistTask.taskId = task.taskId
                    persistTask.title = task.title
                    persistTask.iconNo = task.icon.iconNo
                    persistTask.enabled = task.enabled
                    persistTask.order = task.order
                    persistTask.updatedAt = task.updatedAt.unixTime
                    trans.insertOrUpdate(persistTask)
                }
            }
        }
    }

    fun saveByTaskId(task: Task): Task {
        realmWrapper.getInstance().use { realm ->
            realm.executeTransaction { trans ->
                val persistTask = trans.where(PersistTask::class.java)
                    .equalTo("taskId", task.taskId)
                    .findFirst()

                if (persistTask == null) {
                    trans.insertOrUpdate(task.toPersistTask)
                } else {
                    persistTask.title = task.title
                    persistTask.iconNo = task.icon.iconNo
                    persistTask.enabled = task.enabled
                    persistTask.order = task.order
                    persistTask.updatedAt = task.updatedAt.unixTime
                    trans.insertOrUpdate(persistTask)
                }
            }

            return realm.where(PersistTask::class.java)
                .equalTo("taskId", task.taskId)
                .findFirst()
                ?.let { persistTask ->
                    Task.Builder().build(persistTask)
                }
                ?: let {
                    Timber.e("PersistTaskDao#saveByTaskId: after insert dog not found...")
                    return Task.emptyTask
                }
        }
    }
}