package dev.redicloud.logging

import java.util.ResourceBundle
import java.util.logging.Level

fun getDefaultLogLevel(): Level = getLogLevelByProperty() ?: Level.INFO

fun getLogLevelByProperty(): Level? {
    val property = System.getProperty("redicloud.logging.level")
    return if (property != null) {
        try { Level.parse(property) }catch (_: Exception) { null }
    } else {
        null
    }
}

fun getDefaultLoggingBundle(): ResourceBundle = ResourceBundle.getBundle("redicloud")

fun clearHandlers(logger: Logger) {
    logger.handlers.forEach { logger.removeHandler(it) }
}