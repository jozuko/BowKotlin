package com.studio.jozu.bow.usecase

import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.Event
import com.studio.jozu.bow.domain.ResultType
import com.studio.jozu.bow.domain.Task
import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.domain.extension.CalendarEx.addMonths
import com.studio.jozu.bow.domain.extension.CalendarEx.endMonth
import com.studio.jozu.bow.domain.extension.CalendarEx.isSameMonth
import com.studio.jozu.bow.domain.extension.CalendarEx.startMonth
import com.studio.jozu.bow.domain.extension.CalendarEx.unixTime
import com.studio.jozu.bow.domain.extension.ResponseBodyEx.dump
import com.studio.jozu.bow.infrastructure.SharedHolder
import com.studio.jozu.bow.infrastructure.realm.dao.PersistEventDao
import com.studio.jozu.bow.infrastructure.repository.BowClient
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.util.*

class EventCase(
    private val sharedHolder: SharedHolder,
    private val bowClient: BowClient,
    private val persistEventDao: PersistEventDao,
    private val dogListCase: DogListCase,
    private val taskListCase: TaskListCase,
) {
    fun find(from: Calendar, to: Calendar): Single<List<Event>> {
        val fromMonth = from.startMonth
        val toMonth = to.endMonth
        val isNeedRefresh = isNeedRefresh(fromMonth, toMonth)
        return if (isNeedRefresh) {
            bowClient.getEvents(fromMonth, toMonth)
                .map { response ->
                    val code = response.code()
                    val body = response.body()
                    val events = if (code == 200 && body != null) {
                        body.mapNotNull { Event.Builder().builder(it) }
                    } else {
                        emptyList()
                    }
                    persistEventDao.replace(events, fromMonth, toMonth)
                }
                .map {
                    persistEventDao.find(fromMonth, toMonth)
                }
        } else {
            Single.fromCallable {
                persistEventDao.find(fromMonth, toMonth)
            }
        }
    }

    fun refreshDogIfNeeded(eventList: List<Event>): Single<ResultType> {
        val eventDogIds = eventList.map { it.dogId }
        if (eventDogIds.isEmpty()) {
            return Single.fromCallable { ResultType.SUCCESS }
        }

        return dogListCase.getDogListFromLocal()
            .subscribeOn(Schedulers.io())
            .flatMap { localDogs ->
                var isNeedRefresh = false
                for (eventDogId in eventDogIds) {
                    if (localDogs.find { localDog -> localDog.dogId == eventDogId } == null) {
                        isNeedRefresh = true
                        break
                    }
                }

                dogListCase.refreshDog(force = isNeedRefresh)
            }
    }

    fun refreshTaskIfNeeded(eventList: List<Event>): Single<ResultType> {
        val eventTaskIds = eventList.map { it.taskId }
        if (eventTaskIds.isEmpty()) {
            return Single.fromCallable { ResultType.SUCCESS }
        }

        return taskListCase.getTaskListFromLocal()
            .subscribeOn(Schedulers.io())
            .flatMap { localTasks ->
                var isNeedRefresh = false
                for (eventTaskId in eventTaskIds) {
                    if (localTasks.find { localTask -> localTask.taskId == eventTaskId } == null) {
                        isNeedRefresh = true
                        break
                    }
                }

                taskListCase.refreshTask(force = isNeedRefresh)
            }
    }

    private fun isNeedRefresh(fromMonth: Calendar, toMonth: Calendar): Boolean {
        var month = fromMonth.clone() as Calendar
        val eventRefreshList = sharedHolder.eventRefresh
        val fiveMinutes = 5 * 60
        val now = CalendarEx.now.unixTime

        while (true) {
            val eventRefreshTime = eventRefreshList.find { CalendarEx.fromUnixTime(it.month).isSameMonth(month) } ?: return true
            if (now - eventRefreshTime.updatedAt > fiveMinutes) {
                return true
            }

            if (month.isSameMonth(toMonth)) {
                break
            }
            month = month.addMonths(1)
        }

        return false
    }

    fun add(task: Task, dogs: List<Dog>, timestamp: Calendar): Completable {
        return Completable.fromCallable {
            dogs
                .map { dog ->
                    Event(
                        persistId = null,
                        eventId = "",
                        taskId = task.taskId,
                        dogId = dog.dogId,
                        timestamp = timestamp,
                        updatedAt = CalendarEx.now,
                    )
                }
                .forEach { event ->
                    val response = bowClient.createEventSync(event)
                    if (response.isSuccessful) {
                        val apiModel = response.body()?.let { Event.Builder().builder(it) }
                        if (apiModel != null) {
                            Timber.i("EventCase#add: $event")
                            persistEventDao.save(apiModel)
                        } else {
                            Timber.e("EventCase#add: $event, code=${response.code()}, body is null.")
                        }
                    } else {
                        response.errorBody().dump("EventCase#add", response.code())
                        Timber.e("EventCase#add: $event, code=${response.code()}")
                    }
                }
        }.subscribeOn(Schedulers.io())
    }

    fun delete(event: Event): Completable {
        return Completable.fromCallable {
            persistEventDao.delete(event)
            bowClient.deleteEventSync(event)
        }.subscribeOn(Schedulers.io())
    }

    fun edit(event: Event, task: Task, dogs: List<Dog>, timestamp: Calendar): Completable {
        return delete(event).andThen(add(task, dogs, timestamp))
    }
}