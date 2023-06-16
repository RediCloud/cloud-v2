package dev.redicloud.cluster.file.utils

fun generatePassword(length: Int = 16): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-+#!?"
    return (1..length)
        .map { chars.random() }
        .joinToString("")
}