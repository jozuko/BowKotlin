package com.studio.jozu.bow.usecase

import com.studio.jozu.bow.domain.ResultType
import com.studio.jozu.bow.domain.extension.ResponseBodyEx.dump
import com.studio.jozu.bow.infrastructure.SharedHolder
import com.studio.jozu.bow.infrastructure.realm.RealmWrapper
import com.studio.jozu.bow.infrastructure.repository.BowClient
import com.studio.jozu.bow.infrastructure.repository.model.BowOwnerRes
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import retrofit2.Response

class SignCase(
    private val bowClient: BowClient,
    private val sharedHolder: SharedHolder,
    private val dogListCase: DogListCase,
    private val taskListCase: TaskListCase,
    private val realmWrapper: RealmWrapper,
) {
    val isSignIn: Boolean
        get() = sharedHolder.ownerId.isNotEmpty()

    fun signIn(mailAddress: String): Single<ResultType> {
        return bowClient.ownerSearch(mailAddress)
            .subscribeOn(Schedulers.io())
            .map { response ->
                analyzeResponse(response)
            }
            .flatMap { result ->
                dogListCase.refreshDog(result, force = true)
            }
            .flatMap { result ->
                taskListCase.refreshTask(result)
            }
    }

    fun signUp(mailAddress: String): Single<ResultType> {
        return bowClient.createOwner(mailAddress)
            .subscribeOn(Schedulers.io())
            .map {
                analyzeResponse(it)
            }
            .flatMap { result ->
                dogListCase.refreshDog(result, force = true)
            }
            .flatMap { result ->
                taskListCase.refreshTask(result)
            }
            .flatMap {
                taskListCase.createDefaultTaskIfNeeded(it)
            }
    }

    fun signOut(): Completable {
        return Completable.fromCallable {
            sharedHolder.clearOwnerData()
            realmWrapper.clearAllData()
        }
            .subscribeOn(Schedulers.io())
    }

    private fun analyzeResponse(response: Response<BowOwnerRes>): ResultType {
        val code = response.code()
        val ownerId = response.body()?.ownerId

        if (code == 200 && !ownerId.isNullOrEmpty()) {
            sharedHolder.ownerId = ownerId
            return ResultType.SUCCESS
        }

        response.errorBody().dump("SignCase", code)

        return when (code) {
            404 -> ResultType.NOT_FOUND
            422 -> ResultType.VALIDATION_ERROR
            else -> ResultType.INTERNAL_ERROR
        }
    }
}