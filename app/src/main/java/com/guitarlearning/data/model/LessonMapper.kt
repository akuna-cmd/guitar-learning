package com.guitarlearning.data.model

import com.guitarlearning.domain.model.Lesson

fun LessonDto.toDomain(useEnglishDescription: Boolean): Lesson {
    return Lesson(
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
