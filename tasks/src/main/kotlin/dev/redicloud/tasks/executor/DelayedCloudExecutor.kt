package dev.redicloud.tasks.executor

import dev.redicloud.tasks.CloudTask
import kotlinx.coroutines.delay
import kotlin.time.Duration

class DelayedCloudExecutor(task: CloudTask, val delay: Duration) : CloudTaskExecutor(task) {

    override suspend fun run() {
        delay(delay)
        cloudTask.preExecute()
    }

}