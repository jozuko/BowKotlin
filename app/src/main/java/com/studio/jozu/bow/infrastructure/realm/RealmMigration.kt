package com.studio.jozu.bow.infrastructure.realm

import io.realm.DynamicRealm

class RealmMigration : io.realm.RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
    }
}
