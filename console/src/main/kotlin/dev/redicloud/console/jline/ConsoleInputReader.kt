package dev.redicloud.console.jline

import dev.redicloud.logging.LogManager
import kotlinx.coroutines.delay

class ConsoleInputReader {

    companion object {
        private val LOGGER = LogManager.logger(ConsoleInputReader::class.java)
    }

    private var input: String? = null
    suspend fun readNextInput(): String {
        while (input == null) {
            try {
                delay(500)
            }catch (e: InterruptedException) {
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