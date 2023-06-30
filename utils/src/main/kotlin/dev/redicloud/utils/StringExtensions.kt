package dev.redicloud.utils

import java.util.*

fun String.isUUID(): Boolean {
    return try {
        UUID.fromString(this)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}

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