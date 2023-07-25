package dev.redicloud.console.utils

import dev.redicloud.logging.LogManager
import dev.redicloud.utils.isOpen
import java.io.BufferedReader
import java.io.InputStreamReader

class ScreenProcessHandler(
    private val process: Process,
    private val screen: Screen,
    val filterSpam: Boolean = true
) : Thread(screen.name) {

    companion object {
        val LOGGER = LogManager.logger(ScreenProcessHandler::class)
    }

    init {
        start()
    }

    private val inputStream = process.inputStream
    private val errorStream = process.errorStream
    private val exits = mutableListOf<(Int) -> Unit>()
    private val lines = mutableListOf<(String) -> Unit>()
    private val logged = mutableListOf<String>()

    override fun run() {
        var stopped = false
        while (!stopped) {
            try {
                val inputReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputReader)
                while (process.isAlive && inputStream.isOpen()) {
                    val line = bufferedReader.readLine()
                    if (line == null || line.isEmpty()) continue
                    screen.println(line)
                    lines.forEach { it(line) }
                }
                bufferedReader.close()
                inputReader.close()

                if (errorStream.isOpen()) {
                    val errorReader = InputStreamReader(errorStream)
                    val errorBufferedReader = BufferedReader(errorReader)
                    while (errorStream.isOpen() && errorReader.ready()) {
                        val line = errorBufferedReader.readLine()
                        if (line == null || line.isEmpty()) continue

                        screen.println(line)
                        lines.forEach { it(line) }

                        if (filterSpam) {
                            if (!screen.isActive()) {
                                val identifier = line
                                    .replace(Regex("\\[.*?\\]"), "")
                                    .trim()
                                if (logged.contains(identifier)) return
                                logged.add(identifier)
                            }
                            LOGGER.warning("[${screen.name}]: §c$line")
                        }
                    }
                    errorBufferedReader.close()
                    errorReader.close()
                }

                if (!process.isAlive) {
                    exits.forEach { it(process.exitValue()) }
                    screen.destroy()
                    stopped = true
                }
            }catch (e: Exception) {
                if (!inputStream.isOpen() && !errorStream.isOpen()) return
                LOGGER.severe("Error while reading process output", e)
            }
        }
    }

    suspend fun onExit(): Int = process.waitFor()

    fun onExit(block: (Int) -> Unit) {
        exits.add(block)
    }

    fun onLine(block: (String) -> Unit) {
        lines.add(block)
    }

}