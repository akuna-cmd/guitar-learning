package com.guitarlearning.data.model

import com.guitarlearning.domain.model.Lesson as DomainLesson

fun Lesson.toDomain(useEnglishDescription: Boolean): DomainLesson {
    return DomainLesson(
        id = id,
        level = level,
        order = order,
        title = title,
        description = localizedDescription(useEnglishDescription),
        text = text,
        tabsAscii = tabsAscii,
        tabsGpPath = tabsGpPath
    )
}
