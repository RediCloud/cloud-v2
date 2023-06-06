package dev.redicloud.logging

import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

abstract class Logger(name: String?, resourceBundleName: String? = null) : Logger(name, resourceBundleName) {

    abstract fun forceLog(record: LogRecord)

    abstract var logRecordDispatcher: LogRecordDispatcher?

    fun fine(message: String, throwable: Throwable?, vararg args: Any) {
        this.log(Level.FINE, message, throwable, *args)
    }

    fun finer(message: String, throwable: Throwable?, vararg params: Any) {
        this.log(Level.FINER, message, throwable, params)
    }

    fun finest(message: String, throwable: Throwable?, vararg params: Any) {
        this.log(Level.FINEST, message, throwable, params)
    }

    fun severe(message: String, throwable: Throwable?, vararg params: Any) {
        this.log(Level.SEVERE, message, throwable, params)
    }

    fun warning(message: String, throwable: Throwable?, vararg params: Any) {
        this.log(Level.WARNING, message, throwable, params)
    }

    fun info(message: String, throwable: Throwable?, vararg params: Any) {
        this.log(Level.INFO, message, throwable, params)
    }

    fun config(message: String, throwable: Throwable?, vararg params: Any) {
        this.log(Level.CONFIG, message, throwable, params)
    }

    fun log(level: Level, message: String, throwable: Throwable?, vararg params: Any) {
        if (!this.isLoggable(level)) return
        this.log(level, if (params.size == 0) message else String.format(message, *params), throwable)
    }

}