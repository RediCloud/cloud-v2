package dev.redicloud.utils.gson

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList

inline fun <reified T> Gson.fromJsonToList(json: String): List<T> {
    val type = object : TypeToken<ArrayList<T>>() {}.type
    return fromJson(json, type)
}