package com.studio.jozu.bow.usecase

import android.app.Application
import android.graphics.*
import android.net.Uri
import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.ResultType
import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.domain.extension.CalendarEx.pastMinutes
import com.studio.jozu.bow.domain.extension.ResponseBodyEx.dump
import com.studio.jozu.bow.infrastructure.SharedHolder
import com.studio.jozu.bow.infrastructure.realm.dao.PersistDogDao
import com.studio.jozu.bow.infrastructure.repository.BowClient
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.min

class DogListCase(
    private val application: Application,
    private val bowClient: BowClient,
    private val sharedHolder: SharedHolder,
    private val persistDogDao: PersistDogDao,
) {
    companion object {
        private const val PHOTO_SIZE = 640
    }

    fun getDogListFromLocal(): Single<List<Dog>> {
        return Single.fromCallable { persistDogDao.getAllDogs() }
            .subscribeOn(Schedulers.io())
    }

    fun refreshDog(result: ResultType = ResultType.SUCCESS, force: Boolean = false): Single<ResultType> {
        if (result != ResultType.SUCCESS) {
            return Single.fromCallable { result }
        }

        if (force) {
            sharedHolder.dogRefresh = null
        }

        val prevDogRefresh = sharedHolder.dogRefresh
        if (prevDogRefresh != null && prevDogRefresh.pastMinutes < 5) {
            return Single.fromCallable { result }
        }

        return bowClient.getAllDogs()
            .subscribeOn(Schedulers.io())
            .map { response ->
                val dogResCode = response.code()
                val dogResBody = response.body()

                var dataResult = ResultType.SUCCESS
                if (dogResCode == 200 || dogResCode == 404) {
                    sharedHolder.dogRefresh = CalendarEx.now
                    val replacedDogList = persistDogDao.replaceData(dogResBody?.mapNotNull { apiDog -> Dog.Builder().build(apiDog) } ?: emptyList())
                    replacedDogList
                        .filter { it.imagePath.isNotEmpty() }
                        .forEach { dog ->
                            bowClient.getImage(dog.imagePath)
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                    { imageResponse ->
                                        if (imageResponse.isSuccessful && imageResponse.body() != null) {
                                            val imageBytes = imageResponse.body()!!.bytes()
                                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                            saveDogBitmap(bitmap, dog)
                                        }
                                    },
                                    {
                                        Timber.e("DogListCase#refreshDogIfNeeded - getImage: ${it.localizedMessage}")
                                    }
                                )
                        }
                } else {
                    dataResult = ResultType.INTERNAL_ERROR
                }

                dataResult
            }
    }

    fun updateOrder(dogList: List<Dog>): Single<List<Dog>> {
        return getDogListFromLocal()
            .subscribeOn(Schedulers.io())
            .map { persistDogList ->
                val orderedDogList = dogList.mapIndexed { index, dog ->
                    dog.copy(order = index + 1)
                }

                orderedDogList.filter { filterDog ->
                    val sourceDogOrder = persistDogList.find { it.dogId == filterDog.dogId }?.order ?: return@filter false
                    filterDog.order != sourceDogOrder
                }
            }
            .map { orderChangeDogList ->
                orderChangeDogList.forEach { orderChangeDog ->
                    val response = bowClient.updateDogSync(orderChangeDog)
                    if (!response.isSuccessful) {
                        Timber.e("DogListCase#updateOrder: code=${response.code()}")
                    }
                }
                orderChangeDogList
            }
            .flatMap { orderChangeDogList ->
                if (orderChangeDogList.isEmpty()) {
                    refreshDog(ResultType.NOT_FOUND)
                } else {
                    refreshDog(force = true)
                }
            }
            .flatMap {
                getDogListFromLocal()
            }
    }

    fun changeVisibility(dog: Dog): Single<Dog> {
        return Single.fromCallable {
            dog.enabled = !dog.enabled
            dog
        }
            .subscribeOn(Schedulers.io())
            .flatMap {
                bowClient.updateDog(it)
            }
            .map { response ->
                if (response.isSuccessful) {
                    val apiDog = Dog.Builder().build(response.body()) ?: return@map dog
                    persistDogDao.saveByDogId(apiDog)
                } else {
                    Timber.e("DogListCase#changeVisibility: ${response.code()}")
                    dog
                }
            }
    }

    fun add(dog: Dog, dogPhotoDegree: Float): Single<Dog> {
        // TODO 更新前に犬の重複なまえをチェック
        return if (dog.editingPhotoPath.isEmpty() && dogPhotoDegree == 0F) {
            addNoImage(dog)
        } else {
            addWithImage(dog, dogPhotoDegree)
        }
    }

    private fun addNoImage(dog: Dog): Single<Dog> {
        Timber.d("DogListCase#addNoImage: bowClient.createDog")
        return bowClient.createDog(dog)
            .subscribeOn(Schedulers.io())
            .map { response ->
                val code = response.code()
                val body = response.body()
                Timber.d("DogListCase#addNoImage: bowClient.createDog code=$code, body=$body")

                if (response.isSuccessful) {
                    Dog.Builder().build(body) ?: Dog.emptyDog()
                } else {
                    response.errorBody().dump("DogListCase#addNoImage", code)
                    Dog.emptyDog()
                }
            }
            .map { apiDog ->
                Timber.d("DogListCase#addNoImage: persistDogDao.saveByDogId apiDog=$apiDog")
                if (apiDog.dogId.isNotEmpty()) {
                    persistDogDao.saveByDogId(apiDog)
                } else {
                    Timber.e("DogListCase#addNoImage: persistDogDao.saveByDogId dogId is empty")
                    apiDog
                }
            }
    }

    private fun addWithImage(dog: Dog, dogPhotoDegree: Float): Single<Dog> {
        Timber.d("DogListCase#addWithImage: updateDogImage")
        return updateDogImage(dog, dogPhotoDegree)
            .subscribeOn(Schedulers.io())
            .flatMap { addNoImage(dog) }
    }

    fun edit(dog: Dog, dogPhotoDegree: Float): Single<Dog> {
        // TODO 更新前に犬の重複編集をチェック
        return if (dog.editingPhotoPath.isEmpty() && dogPhotoDegree == 0F) {
            editNoImage(dog)
        } else {
            editWithImage(dog, dogPhotoDegree)
        }
    }

    private fun editNoImage(dog: Dog): Single<Dog> {
        Timber.d("DogListCase#editNoImage: bowClient.updateDog")
        return bowClient.updateDog(dog)
            .map { response ->
                val code = response.code()
                val body = response.body()
                if (code == 200) {
                    Dog.Builder().build(body) ?: Dog.emptyDog()
                } else {
                    response.errorBody().dump("DogListCase#editNoImage", code)
                    Dog.emptyDog()
                }
            }
            .map { apiDog ->
                Timber.d("DogListCase#editNoImage: persistDogDao.saveByDogId")
                if (apiDog.dogId.isNotEmpty()) {
                    persistDogDao.saveByDogId(dog)
                } else {
                    apiDog
                }
            }
    }

    private fun editWithImage(dog: Dog, dogPhotoDegree: Float): Single<Dog> {
        return updateDogImage(dog, dogPhotoDegree)
            .subscribeOn(Schedulers.io())
            .flatMap {
                Timber.d("DogListCase#editWithImage: editNoImage")
                editNoImage(dog)
            }
    }

    private fun updateDogImage(dog: Dog, dogPhotoDegree: Float): Single<Dog> {
        Timber.d("DogListCase#updateDogImage")
        return Single
            .fromCallable {
                Timber.d("DogListCase#updateDogImage: cropRotatePhoto")
                cropRotatePhoto(dog, dogPhotoDegree)
            }
            .flatMap { cropImageFile ->
                Timber.d("DogListCase#updateDogImage: bowClient.uploadImage")
                dog.editingPhotoPath = cropImageFile.absolutePath
                bowClient.uploadImage(cropImageFile)
            }
            .map { response ->
                val code = response.code()
                val imagePath = response.body()?.imagePath
                if (code == 201) {
                    if (imagePath != null) {
                        dog.imagePath = imagePath

                        val fromFile = File(dog.editingPhotoPath)
                        dog.photoPath(application)?.let { toFile ->
                            copyPhotoFile(fromFile, toFile)
                            Timber.d("DogListCase#addWithImage: fromFile=${fromFile.absolutePath}, ${fromFile.length()}")
                            Timber.d("DogListCase#addWithImage: toFile  =${toFile.absolutePath}, ${toFile.length()}")
                        }
                        Timber.d("DogListCase#addWithImage: dog=$dog")
                        dog.editingPhotoPath = ""
                    } else {
                        Timber.e("DogListCase#addWithImage: imagepath is null.")
                    }
                } else {
                    response.errorBody().dump("DogListCase#addWithImage", code)
                }
                dog
            }
    }

    private fun copyPhotoFile(from: File, to: File) {
        createNewFile(to)
        if (!from.exists()) {
            return
        }

        FileInputStream(from).use { inputStream ->
            FileOutputStream(to).use { outputStream ->
                val inputChannel = inputStream.channel
                val outputChannel = outputStream.channel
                var pos = 0L
                while (pos < inputChannel.size()) {
                    pos += inputChannel.transferTo(pos, inputChannel.size(), outputChannel)
                }
            }
        }
    }

    private fun createNewFile(file: File) {
        if (file.parentFile?.exists() == false) {
            file.parentFile?.mkdirs()
        }
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
    }

    fun savePhotoToCache(uri: Uri, dog: Dog) {
        val photoPath = File(application.cacheDir, "dog-photo/${CalendarEx.now.timeInMillis}.jpg")
        createNewFile(photoPath)

        application.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(photoPath).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        dog.editingPhotoPath = photoPath.absolutePath
    }

    fun savePhotoToCache(bitmap: Bitmap, dog: Dog) {
        val photoPath = File(application.cacheDir, "dog-photo/${CalendarEx.now.timeInMillis}.jpg")
        createNewFile(photoPath)

        FileOutputStream(photoPath).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
        dog.editingPhotoPath = photoPath.absolutePath
    }

    private fun cropRotatePhoto(dog: Dog, dogPhotoDegree: Float): File {
        var photoPath = dog.editingPhotoPath
        if (photoPath.isEmpty()) {
            photoPath = dog.photoPath(application)!!.absolutePath
        }

        val bitmap = cropRotateBitmap(photoPath, dogPhotoDegree)
        val cropImageFile = saveCacheBitmap(bitmap, dog.dogId)

        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }

        return cropImageFile
    }

    private fun saveDogBitmap(bitmap: Bitmap, dog: Dog) {
        val photoPath = dog.photoPath(application) ?: return
        saveBitmapToFile(bitmap, photoPath)
    }

    private fun saveCacheBitmap(bitmap: Bitmap, filename: String): File {
        val photoPath = File(application.cacheDir, "dog-photo/${filename}.jpg")
        saveBitmapToFile(bitmap, photoPath)
        return photoPath
    }

    private fun saveBitmapToFile(bitmap: Bitmap, photoPath: File) {
        if (photoPath.parentFile?.exists() == false) {
            photoPath.parentFile?.mkdirs()
        }
        if (photoPath.exists()) {
            photoPath.delete()
        }
        photoPath.createNewFile()

        FileOutputStream(photoPath).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
        }
    }

    private fun cropRotateBitmap(path: String, degree: Float): Bitmap {
        val baseBitmap = decodeSampledBitmapFromResource(path)
        val bitmapSize = min(baseBitmap.width, baseBitmap.height)
        val croppedPoint = Point(baseBitmap.width / 2 - bitmapSize / 2, baseBitmap.height / 2 - bitmapSize / 2)

        return if (degree > 0f) {
            val matrix = Matrix()
            matrix.postRotate(degree)
            Bitmap.createBitmap(baseBitmap, croppedPoint.x, croppedPoint.y, bitmapSize, bitmapSize, matrix, true)
        } else {
            Bitmap.createBitmap(baseBitmap, croppedPoint.x, croppedPoint.y, bitmapSize, bitmapSize)
        }
    }

    private fun decodeSampledBitmapFromResource(filePath: String): Bitmap {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            BitmapFactory.decodeFile(filePath, this)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > PHOTO_SIZE && width > PHOTO_SIZE) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= PHOTO_SIZE && halfWidth / inSampleSize >= PHOTO_SIZE) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}