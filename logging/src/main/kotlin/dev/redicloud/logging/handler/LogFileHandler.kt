package dev.redicloud.logging.handler

import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Level

class LogFileHandler(
    pattern: String,
    limit: Int = LIMIT,
    count: Int = COUNT,
    append: Boolean = true,
) : FileHandler(pattern, limit, count, append) {

    init {
        this.level = Level.ALL
        this.encoding = Charsets.UTF_8.name()
    }

    companion object {
        const val COUNT = 10
        const val LIMIT = 1 shl 25
    }

    fun withFormatter(formatter: Formatter): LogFileHandler {
        super.setFormatter(formatter)
        return this
    }

}