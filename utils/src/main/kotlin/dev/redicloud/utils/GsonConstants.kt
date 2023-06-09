package dev.redicloud.utils

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose

val prettyPrintGson = GsonBuilder().fixKotlinAnnotations().setPrettyPrinting().create()
val gson = GsonBuilder().fixKotlinAnnotations().create()

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

    })
    return this
}