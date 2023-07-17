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

fun String.replaceLast(targetChar: String, replacement: String): String {
    val lastIndexOf = lastIndexOf(targetChar)
    if (lastIndexOf >= 0) {
        return replaceRange(lastIndexOf, lastIndexOf + 1, replacement)
    }
    return this
}