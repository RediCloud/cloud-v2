package dev.redicloud.utils.gson

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible


class InterfaceTypeAdapterFactory(
    private val interfaceClazz: Class<*>,
    private val implClazz: Class<*>
) : TypeAdapterFactory {
    override fun <T : Any> create(
        gson: Gson,
        type: TypeToken<T>
    ): TypeAdapter<T>? {
        if (type.rawType == interfaceClazz) {
            return InterfaceTypeAdapter<T>(implClazz).nullSafe()
        }
        return null
    }
}