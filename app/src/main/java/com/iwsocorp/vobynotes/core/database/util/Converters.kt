package com.iwsocorp.vobynotes.core.database.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.Meaning

class Converters {

    @TypeConverter
    fun fromMark(mark: Mark): String = mark.name

    @TypeConverter
    fun toMark(value: String): Mark = Mark.valueOf(value)

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromMeanings(meanings: List<Meaning>): String {
        return Gson().toJson(meanings)
    }

    @TypeConverter
    fun toMeanings(meaningsJson: String): List<Meaning> {
        val type = object : TypeToken<List<Meaning>>() {}.type
        return Gson().fromJson(meaningsJson, type)
    }

}