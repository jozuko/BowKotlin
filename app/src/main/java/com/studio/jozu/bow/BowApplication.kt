package com.studio.jozu.bow

import android.app.Application
import com.studio.jozu.bow.di.BowComponent
import com.studio.jozu.bow.infrastructure.BowLogger
import timber.log.Timber

class BowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(BowLogger())
        BowComponent.initialize(this)
    }
}