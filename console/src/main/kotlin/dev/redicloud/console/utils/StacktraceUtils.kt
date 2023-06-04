package dev.redicloud.console.utils

fun stacktraceToString(throwable: Throwable, cause: Boolean = false): String {
    val builder = StringBuilder()
    if (!cause) {
        throwable.stackTrace.forEach {
            builder.append(it.toString())
            builder.append("\n")
        }
        if (throwable.cause != null) {
            builder.append(stacktraceToString(throwable.cause!!, true))
        }
    }else {
        builder.append("Caused by: ${throwable.javaClass.name}: ${throwable.message}\n")
        throwable.stackTrace.forEach {
            builder.append(it.toString())
            builder.append("\n")
        }
        if (throwable.cause != null) {
            builder.append(stacktraceToString(throwable.cause!!, true))
        }
    }
    return builder.toString()
}