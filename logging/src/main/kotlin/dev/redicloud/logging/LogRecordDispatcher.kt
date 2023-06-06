package dev.redicloud.logging

import java.util.logging.LogRecord

interface LogRecordDispatcher {

    fun dispatch(logger: Logger, record: LogRecord)

}