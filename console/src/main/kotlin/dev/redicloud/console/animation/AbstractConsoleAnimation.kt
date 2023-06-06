package dev.redicloud.console.animation

import dev.redicloud.console.Console
import org.fusesource.jansi.Ansi
import java.time.Instant

abstract class AbstractConsoleAnimation(
    val updateInterval: Long,
    val staticCursor: Boolean
) : Runnable {

    protected val finishHandlers: MutableList<() -> Unit> = mutableListOf()
    protected var cursorUp = 0
    protected var startInstant: Instant? = null
    var console: Console? = null

    fun addToCursorUp(amount: Int) {
        if (!staticCursor) cursorUp += amount
    }

    fun addFinishHandler(handler: () -> Unit) {
        finishHandlers.add(handler)
    }

    protected fun print(vararg input: String) {
        if (this.console == null) throw IllegalStateException("Console is null")
        if (input.isEmpty()) return
        var ansi = Ansi.ansi().saveCursorPosition().cursorDown(this.cursorUp).eraseLine(Ansi.Erase.ALL)
        input.forEach { ansi = ansi.a(it) }
        this.console!!.writeRaw(ansi.restoreCursorPosition().toString())
    }

    protected fun eraseLastLine() {
        this.console!!.writeRaw(Ansi.ansi().reset().cursorUp(1).eraseLine().toString())
    }

    protected abstract fun handleTick(): Boolean

    override fun run() {
        if (console == null) throw IllegalStateException("Console is null")
        this.startInstant = Instant.now()
        this.console!!.forceWriteLine(System.lineSeparator())
        while (!Thread.currentThread().isInterrupted && !this.handleTick()) {
            try {
                Thread.sleep(this.updateInterval)
            }catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }

    fun handleDone() {
        this.finishHandlers.forEach { it() }
        this.finishHandlers.clear()
    }

    fun staticCursor() = this.staticCursor

    fun getStartInstant() = this.startInstant

    fun getUpdateInterval() = this.updateInterval

    fun getConsole() = this.console

}