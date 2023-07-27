package com.studio.jozu.bow.usecase

import com.studio.jozu.bow.domain.ResultType
import com.studio.jozu.bow.domain.Task
import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.domain.extension.CalendarEx.pastMinutes
import com.studio.jozu.bow.domain.extension.ResponseBodyEx.dump
import com.studio.jozu.bow.infrastructure.SharedHolder
import com.studio.jozu.bow.infrastructure.realm.dao.PersistTaskDao
import com.studio.jozu.bow.infrastructure.repository.BowClient
import com.studio.jozu.bow.infrastructure.repository.model.BowTaskRes
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import retrofit2.Response
import timber.log.Timber

class TaskListCase(
    private val bowClient: BowClient,
    private val sharedHolder: SharedHolder,
    private val persistTaskDao: PersistTaskDao,
) {
    fun getTaskListFromLocal(): Single<List<Task>> {
        return Single.fromCallable { persistTaskDao.getAllTasks() }
            .subscribeOn(Schedulers.io())
    }

    fun refreshTask(result: ResultType = ResultType.SUCCESS, force: Boolean = false): Single<ResultType> {
        if (result != ResultType.SUCCESS) {
            return Single.fromCallable { result }
        }

        if (force) {
            sharedHolder.taskRefresh = null
        }

        val prevTaskRefresh = sharedHolder.taskRefresh
        if (prevTaskRefresh != null && prevTaskRefresh.pastMinutes < 5) {
            return Single.fromCallable { result }
        }

        return bowClient.getAllTasks()
            .subscribeOn(Schedulers.io())
            .map {
                val taskResCode = it.code()
                val taskResBody = it.body()

                var dataResult = ResultType.SUCCESS
                if (taskResCode == 200 || taskResCode == 404) {
                    sharedHolder.taskRefresh = CalendarEx.now
                    val tasks = taskResBody?.mapNotNull { apiTask -> Task.Builder().build(apiTask) } ?: emptyList()
                    persistTaskDao.replaceData(tasks)
                } else {
                    dataResult = ResultType.INTERNAL_ERROR
                }

                dataResult
            }
    }

    fun createDefaultTaskIfNeeded(result: ResultType): Single<ResultType> {
        var singleTask = Single.fromCallable { result }

        if (result != ResultType.SUCCESS) {
            return singleTask
        }
        if (persistTaskDao.count > 0) {
            return singleTask
        }

        Task.defaultTaskList.forEach { defaultTask ->
            singleTask = singleTask
                .flatMap {
                    if (it == ResultType.SUCCESS) {
                        bowClient.createTask(defaultTask)
                    } else {
                        Single.fromCallable { it }
                    }
                }
                .map {
                    if (it is ResultType) {
                        return@map it
                    }

                    if (it is Response<*>) {
                        val code = it.code()
                        val apiTask = it.body() as? BowTaskRes

                        if (code == 201) {
                            val task = Task.Builder().build(apiTask) ?: return@map ResultType.INTERNAL_ERROR
                            persistTaskDao.saveByTaskId(task)
                            return@map ResultType.SUCCESS
                        }
                    }
                    return@map ResultType.INTERNAL_ERROR
                }
        }

        return singleTask
    }

    fun updateOrder(targetTaskList: List<Task>): Single<List<Task>> {
        return getTaskListFromLocal()
            .subscribeOn(Schedulers.io())
            .map { persistTaskList ->
                val orderChangeTaskList = targetTaskList
                    .mapIndexed { index, task ->
                        task.copy(order = index + 1)
                    }
                    .filter { filterTask ->
                        val sourceTaskOrder = persistTaskList.find { it.taskId == filterTask.taskId }?.order ?: return@filter false
                        filterTask.order != sourceTaskOrder
                    }

                orderChangeTaskList.forEach { orderChangeTask ->
                    val response = bowClient.updateTaskSync(orderChangeTask)
                    if (!response.isSuccessful) {
                        Timber.e("TaskListCase#updateOrder: code=${response.code()}")
                    }
                }

                orderChangeTaskList
            }
            .flatMap { orderChangeTaskList ->
                if (orderChangeTaskList.isEmpty()) {
                    refreshTask(ResultType.NOT_FOUND)
                } else {
                    refreshTask(force = true)
                }
            }
            .flatMap {
                getTaskListFromLocal()
            }
    }

    fun changeVisibility(targetTask: Task): Single<Task> {
        return Single.fromCallable {
            targetTask.enabled = !targetTask.enabled
            targetTask
        }
            .subscribeOn(Schedulers.io())
            .flatMap {
                bowClient.updateTask(targetTask)
            }
            .map { response ->
                if (response.isSuccessful) {
                    val apiTask = Task.Builder().build(response.body()) ?: return@map targetTask
                    persistTaskDao.saveByTaskId(apiTask)
                } else {
                    Timber.e("TaskListCase#changeVisibility: ${response.code()}")
                    targetTask
                }
            }
    }

    fun add(targetTask: Task): Single<Task> {
        return bowClient.createTask(targetTask)
            .subscribeOn(Schedulers.io())
            .map { response ->
                val code = response.code()
                val body = response.body()

                val responseTask = if (response.isSuccessful) {
                    Task.Builder().build(body) ?: Task.emptyTask
                } else {
                    response.errorBody().dump("TaskListCase#add", code)
                    Task.emptyTask
                }

                if (responseTask.taskId.isEmpty()) {
                    responseTask
                } else {
                    persistTaskDao.saveByTaskId(responseTask)
                }
            }
    }

    fun edit(targetTask: Task): Single<Task> {
        return bowClient.updateTask(targetTask)
            .subscribeOn(Schedulers.io())
            .map { response ->
                val code = response.code()
                val body = response.body()

                val responseTask = if (response.isSuccessful) {
                    Task.Builder().build(body) ?: Task.emptyTask
                } else {
                    response.errorBody().dump("TaskListCase#edit", code)
                    Task.emptyTask
                }

                if (responseTask.taskId.isEmpty()) {
                    responseTask
                } else {
                    persistTaskDao.saveByTaskId(responseTask)
                }
            }
    }
}