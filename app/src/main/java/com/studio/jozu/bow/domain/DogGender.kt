package com.studio.jozu.bow.domain

enum class DogGender(val genderNo: Int) {
    UNKNOWN(0),
    MALE(1),
    FEMALE(2),
    MALE_SEXLESS(3),
    FEMALE_SEXLESS(4);

    companion object {
        fun getType(genderNo: Int): DogGender {
            return values().find { it.genderNo == genderNo } ?: UNKNOWN
        }
    }
}