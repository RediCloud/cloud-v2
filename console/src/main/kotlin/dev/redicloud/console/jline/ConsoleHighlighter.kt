package dev.redicloud.console.jline

import dev.redicloud.console.Console
import dev.redicloud.console.utils.ConsoleColor
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.reader.impl.DefaultHighlighter
import org.jline.utils.AttributedString
import java.util.regex.Pattern

class ConsoleHighlighter(private val console: Console) : DefaultHighlighter() {

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
            Pattern.compile(it[0]) to try {
                ConsoleColor.valueOf(it[1].uppercase()).ansiCode
            } catch (e: Exception) {
                it[1]
            }
        }

    override fun highlight(reader: LineReader?, buffer: String?): AttributedString {
        if (!enabled || reader == null || buffer == null) return super.highlight(reader, buffer)

        val lastInput = reader.buffer.toString().split(" ").lastOrNull()

        if (lastInput == buffer) return super.highlight(reader, buffer)

        val builder = StringBuilder()
        var prevEnd = 0

        words.forEach { (pattern, replacement) ->
            val matcher = pattern.matcher(buffer.lowercase())
            while (matcher.find()) {
                builder.append(buffer, prevEnd, matcher.start())
                builder.append(replacement)
                prevEnd = matcher.end()
            }
        }
        builder.append(buffer.substring(prevEnd))
        return super.highlight(reader, console.formatText(builder.toString(), "", false))
    }


}