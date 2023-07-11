package dev.redicloud.utils.gson

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class InterfaceTypeAdapter<T : Any>(
    private val implClazz: Class<*>, private val gson: Gson
) : TypeAdapter<T>() {

    override fun write(out: JsonWriter, value: T) {
        out.beginObject()
        out.name("value")
        out.jsonValue(gson.toJson(value))
        out.endObject()
    }

    override fun read(`in`: JsonReader): T {
        var value: T? = null

        `in`.beginObject()
        while (`in`.hasNext()) {
            when (`in`.nextName()) {
                "value" -> {
                    value = gson.fromJson(`in`, implClazz) as? T
                }
                else -> `in`.skipValue()
            }
        }
        `in`.endObject()

        return value ?: throw IllegalArgumentException("Failed to deserialize object")
    }
}