package dev.redicloud.database.repository

import dev.redicloud.database.DatabaseConnection
import org.redisson.api.RBucket
import org.redisson.client.codec.BaseCodec

class DatabaseBucketRepository<T>(
    connection: DatabaseConnection,
    name: String,
    codec: BaseCodec? = null
) : DatabaseRepository<T>(connection, name, codec) {

    suspend fun set(identifier: String, value: T) = getHandle(identifier).set(value)

    suspend fun get(identifier: String): T? = getHandle(identifier).get()

    suspend fun delete(identifier: String): Boolean = getHandle(identifier).delete()

    suspend fun getAll() = connection.client!!.buckets

    fun getHandle(identifier: String): RBucket<T> {
        if (!connection.isConnected()) throw IllegalStateException("Not connected to database")
        return if (codec != null) connection.client!!.getBucket(toDatabaseIdentifier(identifier), codec)
        else connection.client!!.getBucket(toDatabaseIdentifier(identifier))
    }

}