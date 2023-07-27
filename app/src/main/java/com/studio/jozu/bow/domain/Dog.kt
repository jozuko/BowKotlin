package com.studio.jozu.bow.domain

import android.content.Context
import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.domain.extension.CalendarEx.format
import com.studio.jozu.bow.domain.extension.CalendarEx.unixTime
import com.studio.jozu.bow.infrastructure.realm.model.PersistDog
import com.studio.jozu.bow.infrastructure.repository.model.BowDogReq
import com.studio.jozu.bow.infrastructure.repository.model.BowDogRes
import java.io.File
import java.util.*

data class Dog(
    val persistId: String?,
    val dogId: String,
    val name: String,
    val birthday: Calendar?,
    val gender: DogGender,
    var imagePath: String,
    val color: DogColor,
    val order: Int,
    var enabled: Boolean,
    var updatedAt: Calendar,
    var editingPhotoPath: String = "",
) {
    class Builder {
        fun build(persistDog: PersistDog): Dog {
            return Dog(
                persistId = persistDog.recordId,
                dogId = persistDog.dogId,
                name = persistDog.name,
                birthday = if (persistDog.birthday < 0) null else CalendarEx.fromUnixTime(persistDog.birthday),
                gender = DogGender.getType(persistDog.genderNo),
                imagePath = persistDog.imagePath,
                color = DogColor.getType(persistDog.colorNo),
                order = persistDog.order,
                enabled = persistDog.enable,
                updatedAt = CalendarEx.fromUnixTime(persistDog.updatedAt),
            )
        }

        fun build(apiDog: BowDogRes?): Dog? {
            apiDog ?: return null
            val id = apiDog.id ?: return null
            val name = apiDog.name ?: return null
            val birthday = apiDog.birth?.let { CalendarEx.fromUnixTime(it) }
            val gender = DogGender.getType(apiDog.genderNo ?: -1)
            val imagePath = apiDog.imagePath ?: ""
            val color = DogColor.getType(apiDog.colorNo ?: -1)
            val order = apiDog.order ?: 999
            val enable = apiDog.enabled ?: false
            val updatedAt = CalendarEx.fromUnixTime(apiDog.updatedAt)

            return Dog(
                persistId = null,
                dogId = id,
                name = name,
                birthday = birthday,
                gender = gender,
                imagePath = imagePath,
                color = color,
                order = order,
                enabled = enable,
                updatedAt = updatedAt,
            )
        }
    }

    companion object {
        fun emptyDog(): Dog {
            return Dog(
                persistId = null,
                dogId = "",
                name = "",
                birthday = null,
                gender = DogGender.UNKNOWN,
                imagePath = "",
                color = DogColor.RED,
                order = 999,
                enabled = false,
                updatedAt = CalendarEx.now,
            )
        }
    }

    val toPersistDog: PersistDog
        get() = PersistDog(
            recordId = persistId ?: UUID.randomUUID().toString(),
            dogId = dogId,
            name = name,
            birthday = birthday?.unixTime ?: -1,
            genderNo = gender.genderNo,
            imagePath = imagePath,
            colorNo = color.colorNo,
            order = order,
            enable = enabled,
            updatedAt = updatedAt.unixTime
        )

    val toApiRequest: BowDogReq
        get() = BowDogReq(
            name = name,
            birth = birthday?.unixTime ?: -1,
            genderNo = gender.genderNo,
            imagePath = if (imagePath.isEmpty()) null else imagePath,
            colorNo = color.colorNo,
            order = order,
            enabled = enabled,
        )

    fun photoPath(context: Context): File? {
        if (imagePath.isEmpty()) {
            return null
        }

        return File(context.filesDir, "dog_photo/$imagePath")
    }

    fun photo(context: Context): File? {
        if (editingPhotoPath.isNotEmpty()) {
            return File(editingPhotoPath)
        }

        val photoPath = this.photoPath(context)
        return if (photoPath?.exists() == true) {
            photoPath
        } else {
            null
        }
    }

    override fun toString(): String {
        return "Dog(persistId=$persistId," +
                " dogId='$dogId'," +
                " name='$name'," +
                " birthday=${birthday?.format("yyyy/M/d H:m:s") ?: "null"}," +
                " gender=$gender," +
                " imagePath='$imagePath'," +
                " color=$color," +
                " order=$order," +
                " enable=$enabled," +
                " updatedAt=${updatedAt.format("yyyy/M/d H:m:s")}," +
                " editingPhotoPath='$editingPhotoPath')"
    }


}