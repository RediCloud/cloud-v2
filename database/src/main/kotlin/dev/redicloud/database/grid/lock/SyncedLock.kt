package dev.redicloud.database.grid.lock

import dev.redicloud.api.database.grid.lock.ISyncedLock
import dev.redicloud.database.DatabaseConnection
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class SyncedLock(
    override val key: String,
    databaseConnection: DatabaseConnection
) : ISyncedLock {

    private val handle = databaseConnection.client.getLock(key)

    override suspend fun lock() {
        handle.lock()
    }

    override suspend fun lock(duration: Duration) {
        handle.lock(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }

    override suspend fun unlock() {
        handle.unlock()
    }

    override suspend fun forceUnlock(): Boolean {
        return handle.forceUnlock()
    }

    override suspend fun isLocked(): Boolean {
        return handle.isLocked
    }

    override suspend fun isHeldByThread(threadId: Long): Boolean {
        return handle.isHeldByThread(threadId)
    }

    override suspend fun isHeldByCurrentThread(): Boolean {
        return handle.isHeldByCurrentThread
    }

}