package dev.redicloud.api.utils

import com.google.inject.Injector

lateinit var injector: Injector

inline fun <reified T : CloudInjectable> T.injectCloudApi() {
    val membersInjector = injector.getMembersInjector(T::class.java)
    membersInjector.injectMembers(this)
}

interface CloudInjectable