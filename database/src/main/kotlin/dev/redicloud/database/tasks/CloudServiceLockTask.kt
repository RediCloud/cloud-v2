package dev.redicloud.database.tasks

import dev.redicloud.tasks.CloudTask
import org.redisson.api.RLock
import java.util.concurrent.TimeUnit

class CloudServiceLockTask(
    private val lock: RLock
) : CloudTask() {

    override suspend fun execute(): Boolean {
        lock.tryLock(1, 30, TimeUnit.SECONDS)
        return false
    }

}