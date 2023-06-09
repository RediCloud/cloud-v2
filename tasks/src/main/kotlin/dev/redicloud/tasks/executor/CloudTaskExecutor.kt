package dev.redicloud.tasks.executor

import dev.redicloud.tasks.CloudTask
import dev.redicloud.tasks.CloudTaskManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class CloudTaskExecutor(val cloudTask: CloudTask) {

    private var job: Job? = null

    abstract suspend fun run()

    fun run(manager: CloudTaskManager) {
        job = manager.scope.launch {
            run()
        }
    }

    fun cancel() {
        job?.cancel()
    }

}