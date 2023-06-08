package dev.redicloud.tasks.executor

import dev.redicloud.tasks.CloudTask
import kotlinx.coroutines.delay
import kotlin.time.Duration

class PeriodicallyCloudTaskExecutor(
    task: CloudTask,
    val period: Duration,
    val maxExecutions: Int = -1
) : CloudTaskExecutor(task) {

    override suspend fun run() {
        var i = 0
        while (i < maxExecutions || maxExecutions == -1) {
            delay(period)
            i++
            cloudTask.preExecute(this)
        }
    }

}