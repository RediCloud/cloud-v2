package dev.redicloud.api.database.grid.bucket

import kotlin.time.Duration

interface IDataBucket<V> {

    val key: String
    val size: Long
    val exists: Boolean

    suspend fun get(): V?

    suspend fun getAndDelete(): V?

    suspend fun setIfAbsent(block: () -> V): Boolean

    suspend fun setIfAbsent(block: () -> V, expire: Duration): Boolean

    suspend fun setIfExist(block: (V) -> V): Boolean

    suspend fun setIfExist(block: (V) -> V, expire: Duration): Boolean

    suspend fun getAndSet(data: V): V?

    suspend fun getAndSet(data: V, expire: Duration): V?

    suspend fun getAndExpire(expire: Duration): V?

    suspend fun getAndClearExpire(): V?

    suspend fun set(data: V)

    suspend fun delete(): Boolean

}