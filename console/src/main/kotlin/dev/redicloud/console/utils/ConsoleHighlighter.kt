package dev.redicloud.console.utils

import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import java.util.regex.Pattern
import org.jline.reader.impl.DefaultHighlighter
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

class ConsoleHighlighter(val configuration: ConsoleConfiguration) : Highlighter {

    private val default = DefaultHighlighter()

    override fun highlight(reader: LineReader?, buffer: String?): AttributedString {
        val builder = AttributedStringBuilder()
        var currentIndex = 0

        if (buffer == null) return default.highlight(reader, buffer)

        while (currentIndex < buffer.length) {
            var foundMatch = false

            for ((keyword, design) in configuration.wordHighlight) {
                if (buffer.regionMatches(currentIndex, keyword, 0, keyword.length, ignoreCase = true)) {
                    if (design.foreground != null) builder.style(AttributedStyle.DEFAULT.foreground(design.foreground))
                    if (design.background != null) builder.style(AttributedStyle.DEFAULT.background(design.background))
                    builder.append(keyword)
                    currentIndex += keyword.length
                    foundMatch = true
                    break
                }
            }

            if (!foundMatch) {
                builder.append(buffer[currentIndex])
                currentIndex++
            }
        }

        return default.highlight(reader, builder.toAnsi())
    }

    override fun setErrorPattern(errorPattern: Pattern?) = default.setErrorPattern(errorPattern)

    override fun setErrorIndex(errorIndex: Int) = default.setErrorIndex(errorIndex)


}