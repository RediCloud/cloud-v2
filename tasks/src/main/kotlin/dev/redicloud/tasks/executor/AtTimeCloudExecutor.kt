package dev.redicloud.tasks.executor

import dev.redicloud.tasks.CloudTask
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.time.Duration

class AtTimeCloudExecutor(task: CloudTask, val atTime: Long) : CloudTaskExecutor(task) {

    override suspend fun run() {
        val now = System.currentTimeMillis()
        val time = dateTime - now
        if (time > 0) delay(time)
        cloudTask.preExecute()
    }

}