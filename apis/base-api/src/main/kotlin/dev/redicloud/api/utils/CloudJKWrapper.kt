package dev.redicloud.api.utils

class CloudJKWrapper {

    companion object {
        @JvmStatic
        fun injectCloudAPI(clazz: Class<out CloudInjectable>, cloudInjectable: CloudInjectable) {
            injectCloudAPI(clazz, cloudInjectable)
        }

        @JvmStatic
        fun <T> runBlocking(block: suspend () -> T): T = runBlocking {
            block()
        }

    }

}