package dev.redicloud.logging.fallback

import dev.redicloud.utils.logger.Logger
import dev.redicloud.utils.logger.LoggerFactory
import java.util.logging.LogManager

class FallbackLoggerFactory : LoggerFactory {

    private val loggers = mutableMapOf<String, Logger>()

    override fun getLogger(name: String): Logger {
        val registered = LogManager.getLogManager().getLogger(name)

        if (registered is Logger) return registered

        return loggers.computeIfAbsent(name) {
            if (registered == null) {
                FallbackLogger(java.util.logging.Logger.getLogger(name))
            }else {
                FallbackLogger(registered)
            }
        }
    }

}