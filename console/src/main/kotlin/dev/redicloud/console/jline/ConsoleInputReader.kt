package dev.redicloud.console.jline

import kotlinx.coroutines.CompletableDeferred

class ConsoleInputReader {

    private val deferred = CompletableDeferred<String>()

    suspend fun readNextInput(): String = deferred.await()

    fun acceptInput(input: String) {
        deferred.complete(input)
    }
}
