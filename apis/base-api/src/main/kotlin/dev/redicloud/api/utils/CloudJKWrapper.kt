package dev.redicloud.api.utils

class CloudJKWrapper {

    companion object {
        @JvmStatic
        fun <T : CloudInjectable> injectCloudAPI(cloudInjectable: T) {
            val cloudInjectableClazz: Class<T> = cloudInjectable.javaClass
            injectApi(cloudInjectableClazz, cloudInjectable)
        }

        @JvmStatic
        fun <T> runBlocking(block: suspend () -> T): T = kotlinx.coroutines.runBlocking {
            block()
        }

    }

}