package dev.redicloud.logging.handler

import dev.redicloud.logging.LogRecordDispatcher
import dev.redicloud.logging.Logger
import kotlinx.coroutines.delay
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.LogRecord

class ThreadRecordDispatcher(val logger: Logger) : Thread(DEFAULT_NAME.format(logger.name)), LogRecordDispatcher {

    companion object {
        val DEFAULT_NAME = "LR dispatcher %s"
    }

    private val queue: BlockingQueue<LogRecord>
    private var shutdown = false
    private var exited = false

    init {
        this.queue = LinkedBlockingQueue()
        this.isDaemon = true
        this.priority = MIN_PRIORITY
        this.start()
    }

    override fun dispatch(logger: Logger, record: LogRecord) {
        if (this.isInterrupted) return
        this.queue.add(record)
    }

    override fun run() {
        while (!shutdown) {
            try {
                val record = this.queue.take()
                this.logger.forceLog(record)
            } catch (e: InterruptedException) {
                break
            }
        }
        this.queue.forEach { this.logger.forceLog(it) }
        exited = true
        currentThread().interrupt()
    }

    suspend fun shutdown() {
        this.shutdown = true
        var checks = 0
        while (!exited) {
            checks++
            if (checks > 100) {
                this.interrupt()
                break
            }
            delay(100)
        }
    }

}