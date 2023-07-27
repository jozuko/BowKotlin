package com.studio.jozu.bow.presentation.main.list

import java.util.*

class DogEventListModelMonth(val month: Calendar) : DogEventListModel {
    override val viewType: DogEventListViewType
        get() = DogEventListViewType.MONTH
}