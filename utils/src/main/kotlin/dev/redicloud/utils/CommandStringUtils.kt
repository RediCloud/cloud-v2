package dev.redicloud.utils

/**
 * Wraps a value in single quotes for console display.
 *
 * When [colored] is `true`, the value is wrapped with color codes
 * (`%hc%` for highlight color, `%tc%` for text color). When `false`,
 * plain single quotes are used.
 *
 * @param value the value to format
 * @param colored whether to apply color codes (default: `true`)
 * @return the formatted string
 */
fun toConsoleValue(value: Any, colored: Boolean = true): String {
    return if (colored) {
        "§8'%hc%$value§8'%tc%"
    } else {
        "'$value'"
    }
}
