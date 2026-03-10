package com.example.thetest1.data.local

import androidx.room.TypeConverter
import com.example.thetest1.domain.model.PracticedTab
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PracticedTabConverter {
    @TypeConverter
    fun fromPracticedTabList(value: List<PracticedTab>): String {
        val gson = Gson()
        val type = object : TypeToken<List<PracticedTab>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toPracticedTabList(value: String): List<PracticedTab> {
        val gson = Gson()
        val type = object : TypeToken<List<PracticedTab>>() {}.type
        return gson.fromJson(value, type)
    }
}