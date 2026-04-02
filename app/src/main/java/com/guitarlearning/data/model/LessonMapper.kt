package com.guitarlearning.data.model

import com.guitarlearning.domain.model.Lesson as DomainLesson

fun Lesson.toDomain(): DomainLesson {
    return DomainLesson(
        id = id,
        level = level,
        order = order,
        title = title,
        description = description,
        text = text,
        tabsAscii = tabsAscii,
        tabsGpPath = tabsGpPath
    )
}
