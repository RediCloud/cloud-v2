package dev.redicloud.utils.gson

import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import kotlin.reflect.KClass


class InterfaceTypeAdapterFactory : TypeAdapterFactory {

    private val routes: MutableMap<Class<*>, Class<*>> = mutableMapOf()
    private val adapter = mutableMapOf<Class<*>, InterfaceTypeAdapter<*>>()

    fun register(interfaceClass: Class<*>, implClass: Class<*>) {
        if (interfaceClass == implClass) return
        if (routes.containsKey(interfaceClass)) return
        routes[interfaceClass] = implClass
    }

    fun register(interfaceClass: KClass<*>, implClass: KClass<*>) {
        register(interfaceClass.java, implClass.java)
    }

    override fun <T : Any> create(
        gson: Gson,
        type: TypeToken<T>
    ): TypeAdapter<T>? {
        if (routes.containsKey(type.rawType)) {
            return adapter.getOrPut(type.rawType) {
                InterfaceTypeAdapter(
                    routes[type.rawType]!!,
                )
            } as TypeAdapter<T>
        }
        return null
    }

    fun register(fieldAttributes: FieldAttributes) {
        fieldAttributes.getAnnotation(GsonInterface::class.java)?.let {
            register(fieldAttributes.declaredClass, it.value.java)
        }
        fieldAttributes.getAnnotation(GsonGenericInterface::class.java)?.let {
            register(it.interfaceClass.java, it.implClass.java)
        }
    }

}