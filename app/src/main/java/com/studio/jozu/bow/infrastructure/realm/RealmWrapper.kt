package com.studio.jozu.bow.infrastructure.realm

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class RealmWrapper(application: Application) {
    companion object {
        private const val SCHEMA_VERSION = 1L
    }

    init {
        Realm.init(application)
        val config = RealmConfiguration.Builder()
            .schemaVersion(SCHEMA_VERSION)
            .migration(RealmMigration())
            .build()
        Realm.setDefaultConfiguration(config)
    }

    fun getInstance(): Realm {
        return Realm.getDefaultInstance()
    }

    fun clearAllData() {
        getInstance().use { realm ->
            realm.executeTransaction { trans ->
                trans.deleteAll()
            }
        }
    }
}