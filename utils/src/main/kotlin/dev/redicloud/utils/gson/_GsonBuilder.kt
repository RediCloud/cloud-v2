package dev.redicloud.utils.gson

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose

fun GsonBuilder.addInterfaceImpl(factory: InterfaceTypeAdapterFactory): GsonBuilder {
    addSerializationExclusionStrategy(object : ExclusionStrategy {

        override fun shouldSkipClass(clazz: Class<*>?): Boolean {
            return false
        }

        override fun shouldSkipField(f: FieldAttributes?): Boolean {
            f?.let { factory.register(f) }
            return false
        }
    }).addDeserializationExclusionStrategy(object : ExclusionStrategy {
        override fun shouldSkipField(f: FieldAttributes?): Boolean {
            f?.let { factory.register(f) }
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