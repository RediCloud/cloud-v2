package dev.redicloud.logging.handler

import java.util.logging.Handler

abstract class AbstractLogHandler : Handler() {

    override fun flush() {}

    override fun close() {}

}