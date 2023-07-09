package dev.redicloud.utils.gson

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class GsonInterface(val value: KClass<out Any>)