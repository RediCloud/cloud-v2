package dev.redicloud.tasks.executor

import dev.redicloud.tasks.CloudTask

class InstantCloudExecutor(task: CloudTask) : CloudTaskExecutor(task) {

    override suspend fun run() {
        cloudTask.preExecute(this)?.join()
    }
}