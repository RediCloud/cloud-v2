package dev.redicloud.api.utils

class CloudJKWrapper {

    companion object {
        @JvmStatic
        fun <T : CloudInjectable> injectCloudAPI(clazz: Class<T>, cloudInjectable: T) {
            injectApi(clazz, cloudInjectable)
        }

        @JvmStatic
        fun <T> runBlocking(block: suspend () -> T): T = runBlocking {
            block()
        }

    }

}