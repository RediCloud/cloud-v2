package dev.redicloud.database.grid.lock

import dev.redicloud.api.database.grid.lock.ISyncedLock
import dev.redicloud.database.DatabaseConnection
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Distributed lock backed by a Redisson lock, synchronized across the Redis cluster.
 *
 * Provides locking, unlocking, and introspection operations on a named lock
 * identified by [key].
 *
 * @param key the Redis key identifying this lock
 * @param databaseConnection the database connection providing the Redisson client
 */
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
