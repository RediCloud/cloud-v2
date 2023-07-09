package dev.redicloud.utils.gson

import com.google.gson.GsonBuilder
import dev.redicloud.utils.getClassesWithPrefix
import kotlin.reflect.KClass


fun GsonBuilder.registerInterfaceRoute(interfaceClazz: KClass<*>, implClazz: KClass<*>): GsonBuilder {
    registerTypeAdapterFactory(InterfaceTypeAdapterFactory(interfaceClazz.java, implClazz.java))
    return this
}

fun GsonBuilder.scanInterfaceRoutes(packagePrefix: String): GsonBuilder {
    getClassesWithPrefix(packagePrefix).forEach { clazz ->
        listOf(*clazz.fields, *clazz.declaredFields).forEach {
            if (it.isAnnotationPresent(GsonInterface::class.java)) {
                val annotation = it.getAnnotation(GsonInterface::class.java)
                val interfaceClazz = it.type
                val implClazz = annotation.value.java
                registerTypeAdapterFactory(InterfaceTypeAdapterFactory(interfaceClazz, implClazz))
            }
        }
    }
    return this
}