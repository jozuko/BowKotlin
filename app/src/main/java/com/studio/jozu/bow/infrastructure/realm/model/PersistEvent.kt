package com.studio.jozu.bow.infrastructure.realm.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class PersistEvent(
    @PrimaryKey
    open var recordId: String = UUID.randomUUID().toString(),

    open var id: String = "",

    open var dogId: String = "",

    open var taskId: String = "",

    open var timestamp: Int = -1,

    open var updatedAt: Int = -1,
) : RealmObject()