package dev.redicloud.logging.handler

import java.util.logging.Handler

abstract class AbstractLogHandler : Handler() {

    override fun flush() {
        // no-op
    }

    override fun close() {
        // no-op
    }
}
