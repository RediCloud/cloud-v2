package dev.redicloud.console.animation

import dev.redicloud.console.Console
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import org.fusesource.jansi.Ansi
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractConsoleAnimation(
    val updateInterval: Long,
    val staticCursor: Boolean,
    val console: Console
) {

    protected val finishHandlers: MutableList<() -> Unit> = mutableListOf()
    protected val startHandlers: MutableList<() -> Unit> = mutableListOf()
    protected var cursorUp = 0
    protected var startInstant: Instant? = null
    internal var running = false
    protected var firstMessage = false

    fun addToCursorUp(amount: Int) {
        if (!staticCursor) cursorUp += amount
    }

    fun addFinishHandler(handler: () -> Unit) {
        finishHandlers.add(handler)
    }

    fun addStartHandler(handler: () -> Unit) {
        startHandlers.add(handler)
    }

    fun isRunning(): Boolean = running

    protected fun print(input: String) {
        val ansi = Ansi.ansi().saveCursorPosition().cursorUp(this.cursorUp).eraseLine(Ansi.Erase.ALL)
        this.console.writeRaw(input, ensureEndsWith = "\n", eraseLine = false, ansi = ansi, restoreCursor = true)
    }

    protected fun eraseLastLine() {
        this.console.writeRaw(Ansi.ansi().reset().cursorUp(1).eraseLine().toString(), eraseLine = false)
    }

    protected abstract fun handleTick(): Boolean

    suspend fun run() {
        this.startInstant = Instant.now()
        this.console.forceWriteLine(System.lineSeparator())
        var first = false
        while (!this.handleTick() && running) {
            currentCoroutineContext().ensureActive()
            if (!first) this.startHandlers.forEach { it() }
            first = true
            delay(this.updateInterval.milliseconds)
        }
    }

    fun handleDone() {
        this.finishHandlers.forEach { it() }
        this.finishHandlers.clear()
    }
}
