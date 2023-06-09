package dev.redicloud.logging.handler

import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord

class AcceptingLogHandler(val block: (LogRecord, String) -> Unit) : AbstractLogHandler() {

    init {
        this.level = Level.ALL
    }


    override fun publish(record: LogRecord) {
        if (!super.isLoggable(record)) return
        this.block(record, super.getFormatter().format(record))
    }

    fun withFormatter(formatter: Formatter): AcceptingLogHandler {
        super.setFormatter(formatter)
        return this
    }


}