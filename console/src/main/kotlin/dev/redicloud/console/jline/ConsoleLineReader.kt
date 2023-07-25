package dev.redicloud.console.jline

import dev.redicloud.console.Console
import org.jline.reader.impl.LineReaderImpl

class ConsoleLineReader : LineReaderImpl(Console.TERMINAL, "RediCloud-Console", null) {

    override fun historySearchBackward(): Boolean {
        if (Console.CURRENT_CONSOLE == null) return true
        if (Console.CURRENT_CONSOLE!!.matchingHistorySearch) return super.historySearchBackward()
        return if (this.history.previous()) {
            this.setBuffer(this.history.current())
            true
        } else false
    }

    override fun historySearchForward(): Boolean {
        if (Console.CURRENT_CONSOLE == null) return true
        if (Console.CURRENT_CONSOLE!!.matchingHistorySearch) return super.historySearchForward()
        return if (this.history.previous()) {
            this.setBuffer(this.history.current())
            true
        } else false
    }

    override fun upLineOrSearch(): Boolean = this.historySearchBackward()

    override fun downLineOrSearch(): Boolean = this.historySearchForward()

}