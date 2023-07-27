package com.studio.jozu.bow.infrastructure.realm.model

import com.studio.jozu.bow.domain.TaskIcon
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class PersistTask(
    @PrimaryKey
    open var recordId: String = UUID.randomUUID().toString(),

    open var taskId: String = "",

    open var title: String = "",

    open var iconNo: Int = TaskIcon.FOOD.iconNo,

    open var enabled: Boolean = false,

    open var order: Int = 999,

    /** unixtime(second) */
    open var updatedAt: Int = -1,
) : RealmObject()