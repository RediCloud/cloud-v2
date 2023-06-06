package dev.redicloud.logging.fallback

import dev.redicloud.utils.logger.LogManager
import dev.redicloud.utils.logger.LogRecordDispatcher
import dev.redicloud.utils.logger.Logger
import java.util.*
import java.util.logging.Filter
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

class FallbackLogger(val javaLogger: java.util.logging.Logger) : Logger(javaLogger.name, javaLogger.resourceBundleName) {

    override var logRecordDispatcher: LogRecordDispatcher? = null

    override fun log(record: LogRecord) {
        if (this.logRecordDispatcher == null) {
            javaLogger.log(record)
        }else {
            logRecordDispatcher!!.dispatch(this, record)
        }
    }

    override fun forceLog(record: LogRecord) = javaLogger.log(record)

    override fun getLevel(): Level {
        val javaLogLevel = javaLogger.level
        return javaLogLevel ?: LogManager.rootLogger().level
    }

    override fun setLevel(level: Level?) {
        javaLogger.level = level
    }

    override fun getResourceBundle(): ResourceBundle = javaLogger.resourceBundle

    override fun setResourceBundle(bundle: ResourceBundle?) {
        javaLogger.resourceBundle = bundle
    }

    override fun getResourceBundleName(): String = javaLogger.resourceBundleName

    override fun getFilter(): Filter? = javaLogger.filter

    override fun setFilter(filter: Filter?) {
        javaLogger.filter = filter
    }

    override fun isLoggable(level: Level?): Boolean = javaLogger.isLoggable(level)

    override fun addHandler(handler: Handler?) = javaLogger.addHandler(handler)

    override fun removeHandler(handler: Handler?) = javaLogger.removeHandler(handler)

    override fun getHandlers(): Array<Handler> = javaLogger.handlers

    override fun setUseParentHandlers(useParentHandlers: Boolean) {
        javaLogger.useParentHandlers = useParentHandlers
    }

    override fun getUseParentHandlers(): Boolean = javaLogger.useParentHandlers

    override fun getParent(): Logger? {
        val parent = javaLogger.parent
        return if (parent == null) null else FallbackLogger(parent)
    }

    override fun setParent(parent: java.util.logging.Logger?) {
        javaLogger.parent = parent
    }


}