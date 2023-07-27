package com.studio.jozu.bow.domain

import androidx.annotation.DrawableRes
import com.studio.jozu.bow.R

enum class TaskIcon(val iconNo: Int, @DrawableRes val iconRes: Int) {
    FOOD(1, R.mipmap.ic_food),
    BATH(2, R.mipmap.ic_bath),
    HOSPITAL(3, R.mipmap.ic_hospital),
    INJECTION(4, R.mipmap.ic_injection),
    INTRAVENOUS(5, R.mipmap.ic_intravenous),
    MEDICINE1(6, R.mipmap.ic_medicine_1),
    MEDICINE2(7, R.mipmap.ic_medicine_2),
    MEDICINE3(8, R.mipmap.ic_medicine_3),
    MEDICINE4(9, R.mipmap.ic_medicine_4),
    TUBE(10, R.mipmap.ic_tube),
    STOMACH(11, R.mipmap.ic_stomach),
    COLON(12, R.mipmap.ic_colon),
    POOP(13, R.mipmap.ic_poop),
    MOSQUITO(14, R.mipmap.ic_mosquito),
    TICK(15, R.mipmap.ic_tick);

    companion object {
        fun getType(iconNo: Int): TaskIcon {
            return values().find { it.iconNo == iconNo } ?: FOOD
        }
    }
}