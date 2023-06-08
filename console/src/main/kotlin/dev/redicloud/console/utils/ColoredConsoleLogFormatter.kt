package dev.redicloud.console.utils

import dev.redicloud.console.Console
import dev.redicloud.logging.handler.LogFormatter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.logging.Formatter
import java.util.logging.LogRecord


class ColoredConsoleLogFormatter(val console: Console) : Formatter() {

    companion object {
        val DATE_FORMAT = SimpleDateFormat("HH:mm:ss.SSS")
    }

    override fun format(record: LogRecord): String {
        val builder = StringBuilder().append(super.formatMessage(record))

        if (record.thrown != null) {
            val writer = StringWriter()
            record.thrown.printStackTrace(PrintWriter(writer))
            builder.append("\n").append(writer)
        }
        return builder.toString()
    }


}