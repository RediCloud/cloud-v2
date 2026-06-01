package dev.redicloud.utils

import kotlinx.coroutines.*

class ConcurrentBatch(
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher + coroutineExceptionHandler)
    private val actions = mutableListOf<suspend () -> Unit>()
    private val jobs = mutableListOf<Job>()

    fun add(action: suspend () -> Unit) {
        actions.add(action)
    }

    suspend fun joinAll() {
        actions.forEach {
            jobs.add(scope.launch { it() })
        }
        jobs.joinAll()
        scope.cancel()
    }
}
