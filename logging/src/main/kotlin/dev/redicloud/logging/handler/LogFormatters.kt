package dev.redicloud.logging.handler

import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.logging.Formatter
import java.util.logging.LogRecord

val CLEAN_LOG_FORMATTER = LogFormatter(false)
val SEPARATOR_LOG_FORMATTER = LogFormatter(true)
val FILE_LOG_FORMATTER = FileLogFormatter()

class LogFormatter(private val lineSeparator: Boolean) : Formatter() {

    override fun format(record: LogRecord): String {
        val builder = StringBuilder().append(super.formatMessage(record))
        if (lineSeparator) builder.append(System.lineSeparator())
        if (record.thrown != null) {
            val writer = StringWriter()
            record.thrown.printStackTrace(PrintWriter(writer))
            builder.append("\n").append(writer)
        }
        return builder.toString()
    }

}

class FileLogFormatter : Formatter() {

    companion object {
        val TIME_FORMAT: SimpleDateFormat = SimpleDateFormat("HH:mm:ss.SSS")
        val DATE_FORMAT: SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy")
    }

    override fun format(record: LogRecord): String {
        val builder = StringBuilder()
        val message = super.formatMessage(record)
            .replace("§0", "").replace("§1", "").replace("§2", "")
            .replace("§3", "").replace("§4", "").replace("§5", "")
            .replace("§6", "").replace("§7", "").replace("§8", "")
            .replace("§9", "").replace("§a", "").replace("§b", "")
            .replace("§c", "").replace("§d", "").replace("§e", "")
            .replace("§f", "").replace("§l", "").replace("§m", "")
            .replace("§n", "").replace("§o", "").replace("§k", "")
            .replace("§r", "").replace("%hc%", "").replace("%tc%", "")
        builder.append("[").append(DATE_FORMAT.format(record.millis)).append(" ")
            .append(TIME_FORMAT.format(record.millis)).append("] ")
            .append(record.level.name).append(": ")
            .append(message)
        if (record.thrown != null) {
            val writer = StringWriter()
            record.thrown.printStackTrace(PrintWriter(writer))
            builder.append("\n").append(writer)
        }
        return builder.toString()
    }

}
