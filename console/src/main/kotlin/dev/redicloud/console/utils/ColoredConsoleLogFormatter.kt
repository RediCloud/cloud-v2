package dev.redicloud.console.utils

import dev.redicloud.console.Console
import dev.redicloud.logging.handler.LogFormatter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord


class ColoredConsoleLogFormatter(val console: Console, val format: () -> String = { "%message%" } ) : Formatter() {

    companion object {
        val DATE_FORMAT = SimpleDateFormat("HH:mm:ss.SSS")
    }

    override fun format(record: LogRecord): String {
        val builder = StringBuilder()
            .append(format()
                .replace("%date%", LogFormatter.DATE_FORMAT.format(record.millis))
                .replace("%level%", getLevelColor(record.level).ansiCode + record.level.localizedName)
                .replace("%message%", super.formatMessage(record))
            )

        if (record.thrown != null) {
            val writer = StringWriter()
            record.thrown.printStackTrace(PrintWriter(writer))
            builder.append("\n").append(writer)
        }
        return builder.toString()
    }

    private fun getLevelColor(level: Level): ConsoleColor {
        return when(level) {
            Level.INFO -> ConsoleColor.WHITE
            Level.WARNING -> ConsoleColor.YELLOW
            Level.SEVERE -> ConsoleColor.RED
            else -> {
                if (level.intValue() >= Level.FINEST.intValue() && level.intValue() <= Level.FINE.intValue()) {
                    ConsoleColor.BLUE
                }else {
                    ConsoleColor.WHITE
                }
            }
        }
    }


}