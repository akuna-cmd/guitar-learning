package com.example.thetest1.data.model

import com.google.gson.annotations.SerializedName

data class Lesson(
    val id: String,
    val level: String,
    val order: Int,
    val title: String,
    val description: String,
    val text: String,
    @SerializedName("tabs_ascii") val tabsAscii: String,
    @SerializedName("tabs_gp_path") val tabsGpPath: String
)
