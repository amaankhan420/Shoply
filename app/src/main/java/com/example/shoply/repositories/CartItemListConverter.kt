package com.example.shoply.repositories

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CartItemListConverter {
    @TypeConverter
    fun fromCartItemList(items: List<CartItem>?): String {
        return Gson().toJson(items)
    }

    @TypeConverter
    fun toCartItemList(data: String?): List<CartItem> {
        if (data.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<CartItem>>() {}.type
        return Gson().fromJson(data, listType)
    }
}
