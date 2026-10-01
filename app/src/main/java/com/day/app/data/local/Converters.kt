package com.day.app.data.local

import androidx.room.TypeConverter
import com.day.app.domain.model.CacheStatus
import com.day.app.domain.model.DocumentCategory
import com.day.app.domain.model.Priority
import com.day.app.domain.model.ReminderEndCondition
import com.day.app.domain.model.RepeatType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.fromString(value)

    @TypeConverter
    fun fromRepeatType(repeatType: RepeatType): String = repeatType.name

    @TypeConverter
    fun toRepeatType(value: String): RepeatType = RepeatType.fromString(value)

    @TypeConverter
    fun fromReminderEndCondition(condition: ReminderEndCondition): String = condition.name

    @TypeConverter
    fun toReminderEndCondition(value: String): ReminderEndCondition =
        ReminderEndCondition.fromString(value)

    @TypeConverter
    fun fromDocumentCategory(category: DocumentCategory): String = category.name

    @TypeConverter
    fun toDocumentCategory(value: String): DocumentCategory =
        DocumentCategory.fromString(value)

    @TypeConverter
    fun fromCacheStatus(status: CacheStatus): String = status.name

    @TypeConverter
    fun toCacheStatus(value: String): CacheStatus =
        CacheStatus.fromString(value)

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromLongList(list: List<Long>?): String {
        return gson.toJson(list ?: emptyList<Long>())
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
