package dev.redicloud.logging

interface LoggerFactory {

    companion object {
        const val ROOT_LOGGER_NAME: String = ""
    }

    fun getLogger(name: String): Logger

}