package dev.redicloud.api.utils

import com.google.inject.Injector

lateinit var injector: Injector

inline fun <reified T : CloudInjectable> T.injectCloudApi() {
    injectApi(T::class.java, this)
}

fun <T : CloudInjectable> injectApi(clazz: Class<T>, instance: T) {
    val membersInjector = injector.getMembersInjector(clazz)
    membersInjector.injectMembers(instance)
}

interface CloudInjectable