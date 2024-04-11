package dev.redicloud.utils.gson

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.ArrayList

inline fun <reified T> Gson.fromFile(file: File): T {
    if (!file.isFile) {
        throw IllegalArgumentException("File ${file.absolutePath} does not exist")
    }
    return fromJson(file.readText(), T::class.java)
}

inline fun <reified T> Gson.fromStringToList(json: String): List<T> {
    val type = object : TypeToken<ArrayList<T>>() {}.type
    return fromJson(json, type)
}