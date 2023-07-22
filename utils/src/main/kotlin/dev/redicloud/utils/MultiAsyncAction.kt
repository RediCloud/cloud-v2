package dev.redicloud.utils

import kotlinx.coroutines.*
import kotlin.time.Duration

class MultiAsyncAction(
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val scope = CoroutineScope(dispatcher + coroutineExceptionHandler)
    private val actions = mutableListOf<suspend () -> Unit>()
    private val jobs = mutableListOf<Job>()

    fun add(action: suspend () -> Unit) {
        runBlocking { actions.add(action) }
    }

    suspend fun joinAll() {
        runBlocking {
            actions.forEach {
                jobs.add(scope.launch { it() })
            }
            jobs.joinAll()
            scope.cancel()
        }
    }

}