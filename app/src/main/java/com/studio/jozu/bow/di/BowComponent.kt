package com.studio.jozu.bow.di

import android.app.Application
import com.studio.jozu.bow.presentation.dialog.BowAlertDialog
import com.studio.jozu.bow.presentation.dialog.BowDatePickerDialog
import com.studio.jozu.bow.presentation.main.MainActivity
import com.studio.jozu.bow.presentation.main.calendar.CalendarFragment
import com.studio.jozu.bow.presentation.main.list.DogEventListFragment
import com.studio.jozu.bow.presentation.main.settings.dog.SettingDogFragment
import com.studio.jozu.bow.presentation.main.settings.task.SettingTaskFragment
import com.studio.jozu.bow.presentation.sign.SignActivity
import javax.inject.Singleton

@Singleton
@dagger.Component(modules = [BowModule::class, UseCaseModule::class])
interface BowComponent {
    companion object {
        lateinit var instance: BowComponent

        fun initialize(application: Application) {
            if (Companion::instance.isInitialized) {
                return
            }
            instance = DaggerBowComponent.builder()
                .bowModule(BowModule(application))
                .useCaseModule(UseCaseModule(application))
                .build()
        }
    }

    fun inject(target: SignActivity)
    fun inject(target: MainActivity)
    fun inject(target: BowAlertDialog)
    fun inject(target: BowDatePickerDialog)
    fun inject(target: CalendarFragment)
    fun inject(target: DogEventListFragment)
    fun inject(target: SettingDogFragment)
    fun inject(target: SettingTaskFragment)
}