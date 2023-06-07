package dev.redicloud.logging.handler

import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.logging.Formatter
import java.util.logging.LogRecord

class LogFormatter(val lineSeparator: Boolean, val format: (() -> String) = { "%message%" }) : Formatter() {

    companion object {
        val CLEAN = LogFormatter(false)
        val SEPARATOR = LogFormatter(true)
        val DATE_FORMAT: SimpleDateFormat = SimpleDateFormat("HH:mm:ss.SSS")
    }

    override fun format(record: LogRecord): String {
        val builder = StringBuilder()
            builder.append(format().replace("%date%", DATE_FORMAT.format(record.millis))
                .replace("%level%", record.level.localizedName)
                .replace("%message%", super.formatMessage(record)))
        if (lineSeparator) builder.append(System.lineSeparator())
        if (record.thrown != null) {
            val writer = StringWriter()
            record.thrown.printStackTrace(PrintWriter(writer))
            builder.append("\n").append(writer)
        }
        return builder.toString()
    }

}