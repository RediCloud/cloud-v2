package dev.redicloud.utils.gson

import kotlin.reflect.KClass
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class GsonGenericInterface(val interfaceClass: KClass<out Any>, val implClass: KClass<out Any>)