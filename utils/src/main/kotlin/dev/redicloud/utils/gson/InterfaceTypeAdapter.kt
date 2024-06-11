package dev.redicloud.utils.gson

import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class InterfaceTypeAdapter<T : Any>(
    implClazz: Class<T>
) : TypeAdapter<T>() {

    private val adapter = gson.getAdapter(TypeToken.get(implClazz))

    override fun write(out: JsonWriter, value: T?) {
        if (value == null) {
            out.nullValue()
            return
        }
        adapter.write(out, value)
    }

    override fun read(`in`: JsonReader): T? {
        if (`in`.peek() == null) {
            `in`.nextNull()
            return null
        }
        return adapter.read(`in`)
    }
}