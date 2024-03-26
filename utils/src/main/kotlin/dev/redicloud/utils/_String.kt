package dev.redicloud.utils

import java.security.MessageDigest
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

private val md5 = MessageDigest.getInstance("MD5")
fun String.toUUID(): UUID {
    val bytes = md5.digest(this.toByteArray())
    val bigInt = bytes.foldIndexed(0L) { index, acc, byte -> acc or ((byte.toLong() and 0xff) shl (index * 8)) }
    return UUID(bigInt, 0)
}

fun String.toBase64(): String {
    return Base64.getEncoder().encodeToString(toByteArray())
}

fun String.fromBase64(): String {
    return String(Base64.getDecoder().decode(this))
}