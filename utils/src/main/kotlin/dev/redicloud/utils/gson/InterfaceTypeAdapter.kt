package dev.redicloud.utils.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class InterfaceTypeAdapter<T : Any>(
    private val implClazz: Class<*>
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
                    val i = `in`
                    value = gson.fromJson(i, implClazz) as? T
                }
                else -> `in`.skipValue()
            }
        }
        `in`.endObject()

        return value ?: throw IllegalArgumentException("Failed to deserialize object: $`in`")
    }
}