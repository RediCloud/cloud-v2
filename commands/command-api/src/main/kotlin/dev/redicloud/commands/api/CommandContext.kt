package dev.redicloud.commands.api

class CommandContext(val input: String, val annotationArguments: Array<String>) {
    fun <T> getOr(index: Int, default: T): T =
        if (annotationArguments.size > index) annotationArguments[index] as T else default
}