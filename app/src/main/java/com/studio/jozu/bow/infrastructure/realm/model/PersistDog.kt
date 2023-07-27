package com.studio.jozu.bow.infrastructure.realm.model

import com.studio.jozu.bow.domain.DogColor
import com.studio.jozu.bow.domain.DogGender
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class PersistDog(
    @PrimaryKey
    open var recordId: String = UUID.randomUUID().toString(),

    open var dogId: String = "",

    open var name: String = "",

    open var birthday: Int = -1,

    open var genderNo: Int = DogGender.UNKNOWN.genderNo,

    open var imagePath: String = "",

    open var colorNo: Int = DogColor.RED.colorNo,

    open var order: Int = 999,

    open var enable: Boolean = false,

    open var updatedAt: Int = -1,
) : RealmObject() {
    override fun toString(): String {
        return "PersistDog(recordId='$recordId', dogId='$dogId', name='$name', birthday=$birthday, genderNo=$genderNo, imagePath='$imagePath', colorNo=$colorNo, order=$order, enable=$enable, updatedAt=$updatedAt)"
    }
}