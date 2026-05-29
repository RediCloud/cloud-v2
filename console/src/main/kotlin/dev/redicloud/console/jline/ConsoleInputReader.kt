package dev.redicloud.console.jline

import dev.redicloud.logging.LogManager
import kotlinx.coroutines.delay

class ConsoleInputReader {

    companion object {
        private val LOGGER = LogManager.logger(ConsoleInputReader::class.java)
        private const val INPUT_POLL_INTERVAL_MS = 500L
    }

    private var input: String? = null
    suspend fun readNextInput(): String {
        while (input == null) {
            try {
                delay(INPUT_POLL_INTERVAL_MS)
            } catch (e: InterruptedException) {
                LOGGER.severe("Interrupted while waiting for input", e)
                return ""
            }
        }
        return this.input!!
    }
    fun acceptInput(input: String) {
        this.input = input
    }
}
