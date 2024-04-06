package dev.redicloud.api.database.grid.lock

import java.util.concurrent.TimeUnit
import kotlin.time.Duration

interface ISyncedLock {
    val key: String

    suspend fun lock()
    suspend fun unlock()
    suspend fun lock(duration: Duration)
    suspend fun forceUnlock(): Boolean
    suspend fun isLocked(): Boolean
    suspend fun isHeldByThread(threadId: Long): Boolean
    suspend fun isHeldByCurrentThread(): Boolean

}