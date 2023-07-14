package dev.redicloud.utils

fun Boolean.toSymbol(colored: Boolean = true): String {
    return if (this) {
        if (colored) {
            "§2✓"
        } else {
            "✓"
        }
    } else {
        if (colored) {
            "§4✘"
        } else {
            "✘"
        }
    }
}