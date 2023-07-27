package com.studio.jozu.bow.infrastructure

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.studio.jozu.bow.domain.EventRefreshTime
import java.util.*

class SharedHolder(application: Application) {
    enum class Key {
        OWNER_ID,
        EVENT_REFRESH,
        DOG_REFRESH,
        TASK_REFRESH,
        SHOW_CALENDAR,
    }

    private val sharedPref = application.getSharedPreferences("com.studio.jozu.bow", Context.MODE_PRIVATE)

    var ownerId: String
        get() = sharedPref.getString(Key.OWNER_ID.name, null) ?: ""
        set(value) {
            sharedPref.edit {
                putString(Key.OWNER_ID.name, value)
            }
        }

    var eventRefresh: List<EventRefreshTime>
        get() {
            val json = sharedPref.getString(Key.EVENT_REFRESH.name, null) ?: ""
            if (json.isEmpty()) {
                return emptyList()
            }
            val type = object : TypeToken<List<EventRefreshTime>>() {}.type
            return Gson().fromJson(json, type)
        }
        set(value) {
            val json = Gson().toJson(value)
            sharedPref.edit {
                putString(Key.EVENT_REFRESH.name, json)
            }
        }

    var dogRefresh: Calendar?
        get() {
            val time = sharedPref.getLong(Key.DOG_REFRESH.name, -1)
            return if (time < 0) {
                null
            } else {
                Calendar.getInstance().apply { timeInMillis = time }
            }
        }
        set(value) {
            if (value == null) {
                sharedPref.edit {
                    remove(Key.DOG_REFRESH.name)
                }
            } else {
                sharedPref.edit {
                    putLong(Key.DOG_REFRESH.name, value.timeInMillis)
                }
            }
        }


    var taskRefresh: Calendar?
        get() {
            val time = sharedPref.getLong(Key.TASK_REFRESH.name, -1)
            return if (time < 0) {
                null
            } else {
                Calendar.getInstance().apply { timeInMillis = time }
            }
        }
        set(value) {
            if (value == null) {
                sharedPref.edit {
                    remove(Key.TASK_REFRESH.name)
                }
            } else {
                sharedPref.edit {
                    putLong(Key.TASK_REFRESH.name, value.timeInMillis)
                }
            }
        }

    var showCalendar: Boolean
        get() = sharedPref.getBoolean(Key.SHOW_CALENDAR.name, true)
        set(value) = sharedPref.edit {
            putBoolean(Key.SHOW_CALENDAR.name, value)
        }

    fun clearOwnerData() {
        sharedPref.edit {
            remove(Key.OWNER_ID.name)
            remove(Key.EVENT_REFRESH.name)
        }
    }
}