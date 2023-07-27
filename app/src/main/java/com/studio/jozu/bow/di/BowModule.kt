package com.studio.jozu.bow.di

import android.app.Application
import com.studio.jozu.bow.infrastructure.SharedHolder
import com.studio.jozu.bow.infrastructure.event.EventBusManager
import com.studio.jozu.bow.infrastructure.realm.RealmWrapper
import com.studio.jozu.bow.infrastructure.realm.dao.PersistDogDao
import com.studio.jozu.bow.infrastructure.realm.dao.PersistEventDao
import com.studio.jozu.bow.infrastructure.realm.dao.PersistTaskDao
import com.studio.jozu.bow.infrastructure.repository.BowClient
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class BowModule(private val application: Application) {
    @Singleton
    @Provides
    fun provideSharedHolder(
    ) = SharedHolder(application)

    @Singleton
    @Provides
    fun provideEventBus(
    ) = EventBusManager()

    @Singleton
    @Provides
    fun provideBowClient(
        sharedHolder: SharedHolder,
    ) = BowClient(sharedHolder)

    @Singleton
    @Provides
    fun provideRealmWrapper(
    ) = RealmWrapper(application)

    @Singleton
    @Provides
    fun providePersistDogDao(
        realmWrapper: RealmWrapper,
    ) = PersistDogDao(realmWrapper)

    @Singleton
    @Provides
    fun providePersistTaskDao(
        realmWrapper: RealmWrapper,
    ) = PersistTaskDao(realmWrapper)


    @Singleton
    @Provides
    fun providePersistEventDao(
        realmWrapper: RealmWrapper,
    ) = PersistEventDao(realmWrapper)
}