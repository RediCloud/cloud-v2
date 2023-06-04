package dev.redicloud.console.utils

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import dev.redicloud.console.Console

class LoggerListener(private val console: Console) : AppenderBase<ILoggingEvent>(){
    override fun append(event: ILoggingEvent?) {
        if (event == null) return
        val line = LineBuilder.builder(console)
            .source(LineSource.LOG)
            .prefix(event.level.levelStr)

        if (event.throwableProxy != null) {
            line.text(event.throwableProxy.message)
            event.throwableProxy.stackTraceElementProxyArray.forEach {
                line.text(it.toString())
                line.newLine()
            }
            return
        }
        line.text(event.message)
    }

}