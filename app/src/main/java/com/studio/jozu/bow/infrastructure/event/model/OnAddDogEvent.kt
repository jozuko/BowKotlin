package com.studio.jozu.bow.infrastructure.event.model

import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.Task
import java.util.*

data class OnAddDogEvent(
    val task: Task,
    val dogs: List<Dog>,
    val timestamp: Calendar,
)
