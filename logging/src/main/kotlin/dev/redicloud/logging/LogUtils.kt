package dev.redicloud.logging

import ch.qos.logback.classic.LoggerContext
import org.slf4j.helpers.NOPLogger
import org.slf4j.helpers.NOPLoggerFactory
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

fun configureLogger(name: String, level: Level) {
    val context = org.slf4j.LoggerFactory.getILoggerFactory()
    when (context) {
        is LoggerContext -> {
            val logger = context.getLogger(name)
            logger.level = translateLevelToLogback(level)
        }
    }
}

fun translateLevelToLogback(level: Level): ch.qos.logback.classic.Level {
    return when (level) {
        Level.ALL -> ch.qos.logback.classic.Level.ALL
        Level.FINE, Level.FINER -> ch.qos.logback.classic.Level.DEBUG
        Level.SEVERE -> ch.qos.logback.classic.Level.ERROR
        Level.FINEST -> ch.qos.logback.classic.Level.TRACE
        Level.OFF -> ch.qos.logback.classic.Level.OFF
        Level.WARNING -> ch.qos.logback.classic.Level.WARN
        else -> ch.qos.logback.classic.Level.INFO
    }
}

