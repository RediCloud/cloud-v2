package dev.redicloud.console.jline

import dev.redicloud.console.Console
import org.jline.reader.impl.LineReaderImpl

class ConsoleLineReader(private val console: Console) : LineReaderImpl(console.terminal, "RediCloud-Console", null) {

    override fun historySearchBackward(): Boolean {
        if (console.matchingHistorySearch) return super.historySearchBackward()
        return if (this.history.previous()) {
            this.setBuffer(this.history.current())
            true
        } else false
    }

    override fun historySearchForward(): Boolean {
        if (console.matchingHistorySearch) return super.historySearchForward()
        return if (this.history.previous()) {
            this.setBuffer(this.history.current())
            true
        } else false
    }

    override fun upLineOrSearch(): Boolean = this.historySearchBackward()

    override fun downLineOrSearch(): Boolean = this.historySearchForward()

}