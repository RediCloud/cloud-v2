package dev.redicloud.logging

import java.util.logging.Level

fun getDefaultLogLevel(): Level {
    val property = System.getProperty("redicloud.logging.level")
    return if (property != null) {
        try { Level.parse(property) }catch (_: Exception) { Level.INFO }
    } else {
        Level.INFO
    }
}

fun clearHandlers(logger: Logger) {
    logger.handlers.forEach { logger.removeHandler(it) }
}