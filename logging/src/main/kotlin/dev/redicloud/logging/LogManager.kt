package dev.redicloud.logging

import dev.redicloud.logging.fallback.FallbackLoggerFactory
import java.util.ServiceLoader
import kotlin.reflect.KClass

private val LOGGER_FACTORY: LoggerFactory = ServiceLoader.load(LoggerFactory::class.java).firstOrNull() ?: FallbackLoggerFactory()

class LogManager private constructor() {

    companion object {
        fun rootLogger(): Logger = LOGGER_FACTORY.getLogger(LoggerFactory.ROOT_LOGGER_NAME)

        fun logger(name: String): Logger = LOGGER_FACTORY.getLogger(name)

        fun logger(clazz: Class<*>): Logger = LOGGER_FACTORY.getLogger(clazz.name)

        fun logger(clazz: KClass<*>): Logger = LOGGER_FACTORY.getLogger(clazz.qualifiedName!!)

        fun logger(o: Any): Logger = LOGGER_FACTORY.getLogger(o.javaClass.name)
    }

}