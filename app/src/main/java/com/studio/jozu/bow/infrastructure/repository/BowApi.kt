package com.studio.jozu.bow.infrastructure.repository

import com.studio.jozu.bow.infrastructure.repository.model.*
import io.reactivex.rxjava3.core.Single
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface BowApi {
    @POST("owners/")
    fun createOwner(
        @Body body: BowCreateOwnerReq
    ): Single<Response<BowOwnerRes>>

    @GET("owners/search")
    fun isExistOwner(
        @Query("email") mailAddress: String,
    ): Single<Response<BowOwnerRes>>

    @GET("owners/{owner_id}/dogs/")
    fun getAllDogs(
        @Path("owner_id") ownerId: String,
    ): Single<Response<List<BowDogRes>>>

    @POST("owners/{owner_id}/dogs/")
    fun createDog(
        @Path("owner_id") ownerId: String,
        @Body body: BowDogReq,
    ): Single<Response<BowDogRes>>

    @GET("owners/{owner_id}/dogs/{id}")
    fun getDog(
        @Path("owner_id") ownerId: String,
        @Path("id") id: String,
    ): Single<Response<BowDogRes>>

    @PUT("owners/{owner_id}/dogs/{id}")
    fun updateDog(
        @Path("owner_id") ownerId: String,
        @Path("id") id: String,
        @Body body: BowDogReq,
    ): Single<Response<BowDogRes>>

    @PUT("owners/{owner_id}/dogs/{id}")
    fun updateDogSync(
        @Path("owner_id") ownerId: String,
        @Path("id") id: String,
        @Body body: BowDogReq,
    ): Call<BowDogRes>

    @DELETE("owners/{owner_id}/dogs/{id}")
    fun deleteDog(
        @Path("owner_id") ownerId: String,
        @Path("id") id: String,
    ): Single<Response<ResponseBody>>

    @GET("owners/{owner_id}/images/")
    fun getImage(
        @Path("owner_id") ownerId: String,
        @Query("image_path") image_path: String,
    ): Single<Response<ResponseBody>>

    @Multipart
    @POST("owners/{owner_id}/images/")
    fun uploadImage(
        @Path("owner_id") ownerId: String,
        @Part imageFile: MultipartBody.Part,
    ): Single<Response<BowImageRes>>

    @DELETE("owners/{owner_id}/images/")
    fun deleteImage(
        @Path("owner_id") ownerId: String,
        @Query("image_path") image_path: String,
    ): Single<Response<ResponseBody>>

    @GET("owners/{owner_id}/tasks/")
    fun getAllTasks(
        @Path("owner_id") ownerId: String,
    ): Single<Response<List<BowTaskRes>>>

    @POST("owners/{owner_id}/tasks/")
    fun createTask(
        @Path("owner_id") ownerId: String,
        @Body body: BowTaskReq,
    ): Single<Response<BowTaskRes>>

    @GET("owners/{owner_id}/tasks/{id}")
    fun getTask(
        @Path("owner_id") ownerId: String,
        @Path("id") taskId: String,
    ): Single<Response<BowTaskRes>>

    @PUT("owners/{owner_id}/tasks/{id}")
    fun updateTask(
        @Path("owner_id") ownerId: String,
        @Path("id") taskId: String,
        @Body body: BowTaskReq,
    ): Single<Response<BowTaskRes>>

    @PUT("owners/{owner_id}/tasks/{id}")
    fun updateTaskSync(
        @Path("owner_id") ownerId: String,
        @Path("id") taskId: String,
        @Body body: BowTaskReq,
    ): Call<BowTaskRes>

    @DELETE("owners/{owner_id}/tasks/{id}")
    fun deleteTask(
        @Path("owner_id") ownerId: String,
        @Path("id") taskId: String,
    ): Single<Response<ResponseBody>>

    @GET("owners/{owner_id}/events/")
    fun getAllEvent(
        @Path("owner_id") ownerId: String,
        @Query("from") from: Int,
        @Query("to") to: Int,
    ): Single<Response<List<BowEventRes>>>

    @POST("owners/{owner_id}/events/")
    fun createEventSync(
        @Path("owner_id") ownerId: String,
        @Body body: BowEventReq,
    ): Call<BowEventRes>

    @GET("owners/{owner_id}/events/{id}")
    fun getEvent(
        @Path("owner_id") ownerId: String,
        @Path("id") eventId: String,
    ): Single<Response<BowEventRes>>

    @PUT("owners/{owner_id}/events/{id}")
    fun updateEvent(
        @Path("owner_id") ownerId: String,
        @Path("id") eventId: String,
        @Body body: BowEventReq,
    ): Single<Response<BowEventRes>>

    @DELETE("owners/{owner_id}/events/{id}")
    fun deleteEventSync(
        @Path("owner_id") ownerId: String,
        @Path("id") eventId: String,
    ): Call<ResponseBody>
}