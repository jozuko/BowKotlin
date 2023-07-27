package com.studio.jozu.bow.domain

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.studio.jozu.bow.R

enum class DogColor(val colorNo: Int, @ColorRes val colorRes: Int, @DrawableRes val drawableRes: Int) {
    PURPLE(0, R.color.dog_purple, R.drawable.dog_color_purple),
    INDIGO(1, R.color.dog_indigo, R.drawable.dog_color_indigo),
    BLUE(2, R.color.dog_blue, R.drawable.dog_color_blue),
    GREEN(3, R.color.dog_green, R.drawable.dog_color_green),
    YELLOW(4, R.color.dog_yellow, R.drawable.dog_color_yellow),
    ORANGE(5, R.color.dog_orange, R.drawable.dog_color_orange),
    RED(6, R.color.dog_red, R.drawable.dog_color_red);

    companion object {
        fun getType(colorNo: Int): DogColor {
            return values().find { it.colorNo == colorNo } ?: RED
        }
    }
}