package com.guitarlearning.data.model

import com.google.gson.annotations.SerializedName

data class Lesson(
    val id: String,
    val level: String,
    val order: Int,
    val title: String,
    val description: String,
    @SerializedName("description_en") val descriptionEn: String? = null,
    val text: String,
    @SerializedName("tabs_ascii") val tabsAscii: String,
    @SerializedName("tabs_gp_path") val tabsGpPath: String
) {
    fun localizedDescription(useEnglish: Boolean): String {
        return if (useEnglish) descriptionEn?.takeIf { it.isNotBlank() } ?: description else description
    }
}
