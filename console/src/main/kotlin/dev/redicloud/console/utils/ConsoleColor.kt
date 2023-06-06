package dev.redicloud.console.utils

import org.fusesource.jansi.Ansi
import java.util.regex.Pattern


enum class ConsoleColor(
    val displayName: String,
    val index: Char,
    val ansiCode: String
) {
    BLACK("black", '0', Ansi.ansi().reset().fg(Ansi.Color.BLACK).toString()),
    DARK_BLUE("dark_blue", '1', Ansi.ansi().reset().fg(Ansi.Color.BLUE).toString()),
    GREEN("green", '2', Ansi.ansi().reset().fg(Ansi.Color.GREEN).toString()),
    CYAN("cyan", '3', Ansi.ansi().reset().fg(Ansi.Color.CYAN).toString()),
    DARK_RED("dark_red", '4', Ansi.ansi().reset().fg(Ansi.Color.RED).toString()),
    PURPLE("purple", '5', Ansi.ansi().reset().fg(Ansi.Color.MAGENTA).toString()),
    ORANGE("orange", '6', Ansi.ansi().reset().fg(Ansi.Color.YELLOW).toString()),
    GRAY("gray", '7', Ansi.ansi().reset().fg(Ansi.Color.WHITE).toString()),
    DARK_GRAY("dark_gray", '8', Ansi.ansi().reset().fg(Ansi.Color.BLACK).bold().toString()),
    BLUE("blue", '9', Ansi.ansi().reset().fg(Ansi.Color.BLUE).bold().toString()),
    LIGHT_GREEN("light_green", 'a', Ansi.ansi().reset().fg(Ansi.Color.GREEN).bold().toString()),
    AQUA("aqua", 'b', Ansi.ansi().reset().fg(Ansi.Color.CYAN).bold().toString()),
    RED("red", 'c', Ansi.ansi().reset().fg(Ansi.Color.RED).bold().toString()),
    PINK("pink", 'd', Ansi.ansi().reset().fg(Ansi.Color.MAGENTA).bold().toString()),
    YELLOW("yellow", 'e', Ansi.ansi().reset().fg(Ansi.Color.YELLOW).bold().toString()),
    WHITE("white", 'f', Ansi.ansi().reset().fg(Ansi.Color.WHITE).bold().toString()),
    OBFUSCATED("obfuscated", 'k', Ansi.ansi().a(Ansi.Attribute.BLINK_SLOW).toString()),
    BOLD("bold", 'l', Ansi.ansi().a(Ansi.Attribute.UNDERLINE_DOUBLE).toString()),
    STRIKETHROUGH("strikethrough", 'm', Ansi.ansi().a(Ansi.Attribute.STRIKETHROUGH_ON).toString()),
    UNDERLINE("underline", 'n', Ansi.ansi().a(Ansi.Attribute.UNDERLINE).toString()),
    ITALIC("italic", 'o', Ansi.ansi().a(Ansi.Attribute.ITALIC).toString()),
    DEFAULT("default", 'r', Ansi.ansi().reset().toString());

    override fun toString(): String = ansiCode

    companion object {
        private val VALUES = values()
        private const val LOOKUP = "0123456789abcdefklmnor"
        private const val RGB_ANSI = "\u001B[38;2;%d;%d;%dm"
        
        fun toColoredString(triggerChar: Char, input: String): String {
            val contentBuilder = StringBuilder(convertRGBColors(triggerChar, input))
            var breakIndex = contentBuilder.length - 1
            for (i in 0 until breakIndex) {
                val current = contentBuilder[i]
                if (current == triggerChar) {
                    val format = LOOKUP.indexOf(contentBuilder[i + 1])
                    if (format != -1) {
                        val ansiCode = VALUES[format].ansiCode
                        contentBuilder.delete(i, i + 2).insert(i, ansiCode)
                        breakIndex += ansiCode.length - 2
                        return toColoredString(triggerChar, contentBuilder.toString())
                    }
                }
            }
            return contentBuilder.toString()
        }



        private fun convertRGBColors(triggerChar: Char, input: String): String {
            val replacePattern = Pattern.compile("$triggerChar#([\\da-fA-F]){6}")
            val matcher = replacePattern.matcher(input)
            val sb = StringBuilder()
            var lastIndex = 0

            while (matcher.find()) {
                val match = matcher.group()
                val hexInput = Integer.decode(match.substring(1))
                val replacement = String.format(
                    RGB_ANSI,
                    (hexInput shr 16) and 0xFF,
                    (hexInput shr 8) and 0xFF,
                    hexInput and 0xFF
                )
                sb.append(input.substring(lastIndex, matcher.start()))
                sb.append(replacement)
                lastIndex = matcher.end()
            }
            sb.append(input.substring(lastIndex))

            return sb.toString()
        }

        
        fun stripColor(triggerChar: Char, input: String): String {
            val contentBuilder = StringBuilder(stripRGBColors(triggerChar, input))
            var breakIndex = contentBuilder.length - 1
            for (i in 0 until breakIndex) {
                if (contentBuilder[i] == triggerChar && LOOKUP.indexOf(contentBuilder[i + 1]) != -1) {
                    contentBuilder.delete(i, i + 2)
                    breakIndex -= 2
                }
            }
            return contentBuilder.toString()
        }

        
        private fun stripRGBColors(triggerChar: Char, input: String): String {
            val replacePattern = Pattern.compile("$triggerChar#([\\da-fA-F]){6}")
            return replacePattern.matcher(input).replaceAll("")
        }

        fun byChar(index: Char): ConsoleColor? {
            for (color in VALUES) {
                if (color.index == index) {
                    return color
                }
            }
            return null
        }

        fun lastColor(triggerChar: Char, text: String): ConsoleColor? {
            var text = text
            text = text.trim { it <= ' ' }
            return if (text.length > 2 && text[text.length - 2] == triggerChar) {
                byChar(text[text.length - 1])
            } else null
        }
    }
}