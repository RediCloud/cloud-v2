package dev.redicloud.logging.handler

import dev.redicloud.logging.LogRecordDispatcher
import dev.redicloud.logging.Logger
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.LogRecord

class ThreadRecordDispatcher(val logger: Logger) : Thread(DEFAULT_NAME.format(logger.name)), LogRecordDispatcher {

    companion object {
        val DEFAULT_NAME = "LR dispatcher %s"
    }

    private val queue: BlockingQueue<LogRecord>

    init {
        this.queue = LinkedBlockingQueue()
        this.isDaemon = true
        this.priority = Thread.MIN_PRIORITY
        this.start()
    }

    override fun dispatch(logger: Logger, record: LogRecord) {
        if (this.isInterrupted) return
        this.queue.add(record)
    }

    override fun run() {
        while (!super.isInterrupted()) {
            try {
                val record = this.queue.take()
                this.logger.forceLog(record)
            } catch (e: InterruptedException) {
                break
            }
        }
        this.queue.forEach { this.logger.forceLog(it) }
        currentThread().interrupt()
    }

}