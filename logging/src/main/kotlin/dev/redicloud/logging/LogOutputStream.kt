package dev.redicloud.logging

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level

class LogOutputStream(val level: Level, val logger: Logger) : ByteArrayOutputStream() {

    companion object {
        val AUTO_FLUSH = true
        val CHARSET = Charsets.UTF_8
        fun forSevere(logger: Logger) = LogOutputStream(Level.SEVERE, logger)
        fun forInformation(logger: Logger) = LogOutputStream(Level.INFO, logger)
    }

    private val lock: Lock = ReentrantLock()

    fun toPrintStream(): PrintStream = this.toPrintStream(AUTO_FLUSH, CHARSET)

    fun toPrintStream(autoFlush: Boolean, charset: Charset) = PrintStream(this, autoFlush, charset.name())

    override fun flush() {
        lock.lock()
        try {
            super.flush()
            val content = this.toString(Charsets.UTF_8.name())
            super.reset()

            if (content.isNotEmpty() && !content.equals(System.lineSeparator())) {
                this.logger.log(this.level, content)
            }
        }finally {
            lock.unlock()
        }
    }


}