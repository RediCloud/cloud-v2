package dev.redicloud.database.grid.bucket

import dev.redicloud.api.database.grid.bucket.IDataBucket
import dev.redicloud.database.DatabaseConnection
import kotlin.time.Duration

/**
 * A data bucket backed by a Redisson bucket, providing atomic get/set/delete
 * operations on a single Redis key.
 *
 * Supports expiration, conditional set (if-exists / if-absent), and get-and-modify operations.
 *
 * @param V the value type stored in this bucket
 * @param key the Redis key identifying this bucket
 * @param databaseConnection the database connection providing the Redisson client
 */
class DataBucket<V>(
    override val key: String,
    databaseConnection: DatabaseConnection
) : IDataBucket<V> {

    private val handle = databaseConnection.client.getBucket<V>(key)

    override val size: Long
        get() = handle.size()

    override val exists: Boolean
        get() = handle.isExists

    override suspend fun get(): V? =
        handle.get()

    override suspend fun getAndDelete(): V? =
        handle.getAndDelete()

    override suspend fun getAndExpire(expire: Duration): V? =
        handle.getAndExpire(java.time.Duration.ofMillis(expire.inWholeMilliseconds))

    override suspend fun getAndClearExpire(): V? =
        handle.getAndClearExpire()

    override suspend fun set(data: V) =
        handle.set(data)

    override suspend fun getAndSet(data: V, expire: Duration): V? =
        handle.getAndSet(data, java.time.Duration.ofMillis(expire.inWholeMilliseconds))

    override suspend fun getAndSet(data: V): V? =
        handle.getAndSet(data)

    override suspend fun setIfExist(block: (V) -> V, expire: Duration): Boolean {
        val value = handle.get() ?: return false
        handle.set(block(value), java.time.Duration.ofMillis(expire.inWholeMilliseconds))
        return true
    }

    override suspend fun setIfExist(block: (V) -> V): Boolean {
        val value = handle.get() ?: return false
        handle.set(block(value))
        return true
    }

    override suspend fun setIfAbsent(block: () -> V, expire: Duration): Boolean {
        if (handle.isExists) return false
        handle.set(block(), java.time.Duration.ofMillis(expire.inWholeMilliseconds))
        return true
    }

    override suspend fun setIfAbsent(block: () -> V): Boolean {
        if (handle.isExists) return false
        handle.set(block())
        return true
    }

    override suspend fun delete(): Boolean =
        handle.delete()
}
