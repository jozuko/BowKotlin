package com.studio.jozu.bow.infrastructure.event

import org.greenrobot.eventbus.EventBus

class EventBusManager {
    private val rawInstance: EventBus
        get() = EventBus.getDefault()

    fun register(subscriber: Any) {
        val instance = rawInstance
        if (!instance.isRegistered(subscriber)) {
            instance.register(subscriber)
        }
    }

    fun unregister(subscriber: Any) {
        val instance = rawInstance
        if (instance.isRegistered(subscriber)) {
            instance.unregister(this)
        }
    }

    fun post(event: Any) {
        rawInstance.post(event)
    }
}