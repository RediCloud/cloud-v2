package dev.redicloud.commands.api

fun String.removeFirstSpaces(): String {
    var i = 0
    while (i < this.length && this[i] == ' ') {
        i++
    }
    return this.substring(i)
}

fun String.removeLastSpaces(): String {
    var i = this.length - 1
    while (i >= 0 && this[i] == ' ') {
        i--
    }
    return this.substring(0, i + 1)
}