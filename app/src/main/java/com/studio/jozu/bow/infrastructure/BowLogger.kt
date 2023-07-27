package com.studio.jozu.bow.infrastructure

import android.util.Log
import com.studio.jozu.bow.BuildConfig
import timber.log.Timber

class BowLogger : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!BuildConfig.DEBUG && priority <= Log.DEBUG) {
            return
        }
        super.log(priority, tag, "[Bow]::$message", t)
    }
}