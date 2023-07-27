package com.studio.jozu.bow.presentation.main.list

import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.Event
import com.studio.jozu.bow.domain.Task

class DogEventListModelEvent(val event: Event, val dog: Dog, val task: Task) : DogEventListModel {
    override val viewType: DogEventListViewType
        get() = DogEventListViewType.EVENT


}