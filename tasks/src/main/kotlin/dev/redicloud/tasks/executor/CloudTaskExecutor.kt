package dev.redicloud.tasks.executor

import dev.redicloud.tasks.CloudTask
import dev.redicloud.tasks.CloudTaskManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class CloudTaskExecutor(val cloudTask: CloudTask) {

    private var job: Job? = null
    private val blockQueue = mutableListOf<() -> Unit>()

    abstract suspend fun run()

    fun run(manager: CloudTaskManager) {
        job = manager.scope.launch {
            run()
            manager.unregister(this@CloudTaskExecutor.cloudTask.id)
        }
        this.blockQueue.forEach { onFinished(it) }
    }

    fun cancel() {
        job?.cancel()
    }

    fun onFinished(block: () -> Unit) {
        if (job == null) {
            blockQueue.add(block)
            return
        }
        job!!.invokeOnCompletion { block() }
    }

}