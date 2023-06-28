package dev.redicloud.utils

import java.io.InputStream

fun InputStream.isOpen(): Boolean = try {
    this.available()
    true
} catch (e: Exception) {
    false
}