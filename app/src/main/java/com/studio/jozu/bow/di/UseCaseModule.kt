package com.studio.jozu.bow.di

import android.app.Application
import com.studio.jozu.bow.infrastructure.SharedHolder
import com.studio.jozu.bow.infrastructure.realm.RealmWrapper
import com.studio.jozu.bow.infrastructure.realm.dao.PersistDogDao
import com.studio.jozu.bow.infrastructure.realm.dao.PersistEventDao
import com.studio.jozu.bow.infrastructure.realm.dao.PersistTaskDao
import com.studio.jozu.bow.infrastructure.repository.BowClient
import com.studio.jozu.bow.usecase.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class UseCaseModule(private val application: Application) {
    @Singleton
    @Provides
    fun provideSignCase(
        bowClient: BowClient,
        sharedHolder: SharedHolder,
        dogListCase: DogListCase,
        taskListCase: TaskListCase,
        realmWrapper: RealmWrapper,
    ) = SignCase(bowClient, sharedHolder, dogListCase, taskListCase, realmWrapper)

    @Singleton
    @Provides
    fun provideDogListCase(
        bowClient: BowClient,
        sharedHolder: SharedHolder,
        persistDogDao: PersistDogDao,
    ) = DogListCase(application, bowClient, sharedHolder, persistDogDao)

    @Singleton
    @Provides
    fun provideTaskListCase(
        bowClient: BowClient,
        sharedHolder: SharedHolder,
        persistTaskDao: PersistTaskDao,
    ) = TaskListCase(bowClient, sharedHolder, persistTaskDao)

    @Singleton
    @Provides
    fun provideCalendarCase(
    ) = CalendarCase()

    @Singleton
    @Provides
    fun provideEventCase(
        sharedHolder: SharedHolder,
        bowClient: BowClient,
        persistEventDao: PersistEventDao,
        dogListCase: DogListCase,
        taskListCase: TaskListCase,
    ) = EventCase(sharedHolder, bowClient, persistEventDao, dogListCase, taskListCase)
}