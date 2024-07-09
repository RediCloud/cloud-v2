package dev.redicloud.api.utils

import kotlinx.coroutines.runBlocking

class CloudJKWrapper {

    companion object {
        @JvmStatic
        fun injectCloudAPI(cloudInjectable: CloudInjectable) {
            cloudInjectable.injectCloudApi()
        }

        @JvmStatic
        fun <T> runBlocking(block: suspend () -> T): T = runBlocking {
            block()
        }

    }

}