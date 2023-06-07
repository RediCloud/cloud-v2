package dev.redicloud.console.jline

import dev.redicloud.console.Console
import dev.redicloud.console.utils.ConsoleColor
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.reader.impl.DefaultHighlighter
import org.jline.utils.AttributedString
import java.util.regex.Pattern

class ConsoleHighlighter(private val console: Console) : Highlighter {

    private val default = DefaultHighlighter()
    private val enabled = System.getProperty("redicloud.console.highlight.enabled", "true").toBoolean()
    private val words = System.getProperty(
        "redicloud.console.highlight.words",
        "success:red;" +
                "warn:yellow;" +
                "info:white;" +
                "debug:blue;" +
                "fail:red;" +
                "fatal:darK_red;" +
                "trace:blue;" +
                "running:green;" +
                "stopped:red;" +
                "starting:yellow;" +
                "stopping:yellow;" +
                "started:green"
    ).split(";").map { it.split(":") }
        .associate {
            it[0] to try {
                (ConsoleColor.valueOf(it[1].uppercase()))
            } catch (e: Exception) {
                ConsoleColor.WHITE
            }
        }

    override fun highlight(reader: LineReader?, buffer: String?): AttributedString {
        if (!enabled) return default.highlight(reader, buffer)
        return default.highlight(reader, console.formatText(buffer ?: "", "", false))
    }

    override fun setErrorPattern(errorPattern: Pattern?) = default.setErrorPattern(errorPattern)

    override fun setErrorIndex(errorIndex: Int) = default.setErrorIndex(errorIndex)


}