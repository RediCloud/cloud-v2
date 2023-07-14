package dev.redicloud.console.utils

fun toConsoleValue(value: Any, colored: Boolean = true): String {
    return if (colored) {
        "§8'%hc%${value.toString()}§8'%tc%"
    }else {
        "'${value.toString()}'"
    }
}