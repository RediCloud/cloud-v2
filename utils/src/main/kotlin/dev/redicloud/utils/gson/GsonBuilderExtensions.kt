package dev.redicloud.utils.gson

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import dev.redicloud.utils.getClassesWithPrefix
import kotlin.reflect.KClass

fun GsonBuilder.addInterfaceImpl(): GsonBuilder {
    val factory = InterfaceTypeAdapterFactory()
    addSerializationExclusionStrategy(object : ExclusionStrategy {

        override fun shouldSkipClass(clazz: Class<*>?): Boolean {
            return false
        }

        override fun shouldSkipField(f: FieldAttributes?): Boolean {
            f?.getAnnotation(GsonInterface::class.java)?.let {
                factory.register(f.declaredClass, it.value.java)
            }
            f?.getAnnotation(GsonGenericInterface::class.java)?.let {
                factory.register(it.interfaceClass.java, it.implClass.java)
            }
            return false
        }
    }).addDeserializationExclusionStrategy(object : ExclusionStrategy {
        override fun shouldSkipField(f: FieldAttributes?): Boolean {
            f?.getAnnotation(GsonInterface::class.java)?.let {
                factory.register(f.declaredClass, it.value.java)
            }
            f?.getAnnotation(GsonGenericInterface::class.java)?.let {
                factory.register(it.interfaceClass.java, it.implClass.java)
            }
            return false
        }

        override fun shouldSkipClass(clazz: Class<*>?): Boolean {
            return false
        }

    })
    return registerTypeAdapterFactory(factory)
}

fun GsonBuilder.fixKotlinAnnotations(): GsonBuilder {
    addSerializationExclusionStrategy(object : ExclusionStrategy {

        override fun shouldSkipField(f: FieldAttributes?): Boolean =
            f?.getAnnotation(Expose::class.java)?.serialize == false

        override fun shouldSkipClass(p0: Class<*>?): Boolean =
            p0?.getAnnotation(Expose::class.java)?.serialize == false

    }).addDeserializationExclusionStrategy(object : ExclusionStrategy {

        override fun shouldSkipField(f: FieldAttributes?): Boolean =
            f?.getAnnotation(Expose::class.java)?.deserialize == false

        override fun shouldSkipClass(clazz: Class<*>?): Boolean =
            clazz?.getAnnotation(Expose::class.java)?.deserialize == false

    }).serializeNulls()
    return this
}