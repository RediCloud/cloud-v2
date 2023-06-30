package dev.redicloud.commands.api

fun String.isOptionalArgument(): Boolean = this.startsWith("[") && this.endsWith("]")

fun String.isRequiredArgument(): Boolean = this.startsWith("<") && this.endsWith(">")

fun String.isArgument(): Boolean = isOptionalArgument() || isRequiredArgument()