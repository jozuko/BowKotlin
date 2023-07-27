package com.studio.jozu.bow.infrastructure.realm.dao

import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.extension.CalendarEx.unixTime
import com.studio.jozu.bow.infrastructure.realm.RealmWrapper
import com.studio.jozu.bow.infrastructure.realm.model.PersistDog
import io.realm.Sort
import timber.log.Timber

class PersistDogDao(private val realmWrapper: RealmWrapper) {
    fun replaceData(dogs: List<Dog>): List<Dog> {

        realmWrapper.getInstance().use { realm ->
            val replacedDogList = mutableListOf<Dog>()

            realm.executeTransaction { trans ->
                val persistList = dogs.map { it.toPersistDog }

                persistList.forEach { persistDog ->
                    val findDog = trans.where(PersistDog::class.java)
                        .equalTo("dogId", persistDog.dogId)
                        .findFirst()

                    if (findDog == null) {
                        replacedDogList.add(Dog.Builder().build(persistDog))
                    } else if (findDog.updatedAt != persistDog.updatedAt) {
                        persistDog.recordId = findDog.recordId
                        replacedDogList.add(Dog.Builder().build(persistDog))
                    }
                }

                trans.delete(PersistDog::class.java)
                persistList.forEach {
                    trans.insertOrUpdate(it)
                }
            }

            return replacedDogList
        }
    }

    fun getAllDogs(): List<Dog> {
        realmWrapper.getInstance().use { realm ->
            return realm.where(PersistDog::class.java)
                .findAll()
                .sort("order", Sort.ASCENDING)
                .map {
                    Dog.Builder().build(it)
                }
        }
    }

    fun saveByDogId(dog: Dog): Dog {
        realmWrapper.getInstance().use { realm ->
            realm.executeTransaction { trans ->
                val persistDog = trans.where(PersistDog::class.java)
                    .equalTo("dogId", dog.dogId)
                    .findFirst() ?: dog.toPersistDog

                persistDog.name = dog.name
                persistDog.birthday = dog.birthday?.unixTime ?: -1
                persistDog.genderNo = dog.gender.genderNo
                persistDog.colorNo = dog.color.colorNo
                persistDog.enable = dog.enabled
                persistDog.imagePath = dog.imagePath
                persistDog.order = dog.order
                persistDog.updatedAt = dog.updatedAt.unixTime

                trans.insertOrUpdate(persistDog)
            }

            return realm.where(PersistDog::class.java)
                .equalTo("dogId", dog.dogId)
                .findFirst()
                ?.let { persistDog ->
                    Timber.d("PersistDogDao#saveByDogId: after insert persistDog=$persistDog")
                    Dog.Builder().build(persistDog)
                }
                ?: let {
                    Timber.e("PersistDogDao#saveByDogId: after insert dog not found...")
                    return Dog.emptyDog()
                }
        }
    }
}