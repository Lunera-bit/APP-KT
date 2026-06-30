package com.cyryel.data.local

import androidx.room.TypeConverter
import com.cyryel.data.product.ProductVariant
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromVariantList(value: List<ProductVariant>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toVariantList(value: String): List<ProductVariant> {
        val type = object : TypeToken<List<ProductVariant>>() {}.type
        return gson.fromJson(value, type)
    }
}
