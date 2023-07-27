package com.studio.jozu.bow.infrastructure.repository

import androidx.core.text.htmlEncode
import com.studio.jozu.bow.BuildConfig
import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.Event
import com.studio.jozu.bow.domain.Task
import com.studio.jozu.bow.domain.extension.CalendarEx.unixTime
import com.studio.jozu.bow.infrastructure.SharedHolder
import com.studio.jozu.bow.infrastructure.repository.model.*
import io.reactivex.rxjava3.core.Single
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class BowClient(private val sharedHolder: SharedHolder) {
    companion object {
        private const val BASE_URL = "https://bow-api.jozuo.work/"
    }

    private val logger = HttpLoggingInterceptor { message -> Timber.i("BowClient: $message") }.apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.BASIC
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(240, TimeUnit.SECONDS)
        .connectTimeout(240, TimeUnit.SECONDS)
        .writeTimeout(240, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(BowApiHeaderInterceptor())
        .addInterceptor(logger)
        .build()

    private val retrofit = Retrofit.Builder()
        .client(httpClient)
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
        .build()

    private val apiClient = retrofit.create(BowApi::class.java)

    private val ownerId: String
        get() = sharedHolder.ownerId

    fun ownerSearch(mailAddress: String): Single<Response<BowOwnerRes>> {
        return apiClient.isExistOwner(mailAddress.htmlEncode())
    }

    fun createOwner(mailAddress: String): Single<Response<BowOwnerRes>> {
        val body = BowCreateOwnerReq(mailAddress = mailAddress)
        return apiClient.createOwner(body)
    }

    fun getAllDogs(): Single<Response<List<BowDogRes>>> {
        return apiClient.getAllDogs(ownerId)
    }

    fun getAllTasks(): Single<Response<List<BowTaskRes>>> {
        return apiClient.getAllTasks(ownerId)
    }

    fun createTask(task: Task): Single<Response<BowTaskRes>> {
        return apiClient.createTask(ownerId, task.toRequestApiModel)
    }

    fun getEvents(from: Calendar, to: Calendar): Single<Response<List<BowEventRes>>> {
        return apiClient.getAllEvent(ownerId, from.unixTime, to.unixTime)
    }

    fun createDog(dog: Dog): Single<Response<BowDogRes>> {
        return apiClient.createDog(ownerId, dog.toApiRequest)
    }

    fun updateDog(dog: Dog): Single<Response<BowDogRes>> {
        return apiClient.updateDog(ownerId, dog.dogId, dog.toApiRequest)
    }

    fun updateDogSync(dog: Dog): Response<BowDogRes> {
        return apiClient.updateDogSync(ownerId, dog.dogId, dog.toApiRequest).execute()
    }

    fun getImage(imagePath: String): Single<Response<ResponseBody>> {
        return apiClient.getImage(ownerId, imagePath)
    }

    fun uploadImage(file: File): Single<Response<BowImageRes>> {
        val requestFile = file.asRequestBody("image/jpeg".toMediaType())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        return apiClient.uploadImage(ownerId, body)
    }

    fun updateTaskSync(task: Task): Response<BowTaskRes> {
        return apiClient.updateTaskSync(ownerId, task.taskId, task.toRequestApiModel).execute()
    }

    fun updateTask(task: Task): Single<Response<BowTaskRes>> {
        return apiClient.updateTask(ownerId, task.taskId, task.toRequestApiModel)
    }

    fun createEventSync(event: Event): Response<BowEventRes> {
        return apiClient.createEventSync(ownerId, event.toRequestApiModel).execute()
    }

    fun deleteEventSync(event: Event): Response<ResponseBody> {
        return apiClient.deleteEventSync(ownerId, eventId = event.eventId).execute()
    }
}