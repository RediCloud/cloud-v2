package dev.redicloud.utils

import com.google.inject.AbstractModule
import com.google.inject.binder.AnnotatedBindingBuilder
import kotlin.reflect.KClass

class InjectorModule : AbstractModule() {

    fun <T : Any> bind(clazz: KClass<T>): AnnotatedBindingBuilder<T>? {
        return bind(clazz.java)
    }

}