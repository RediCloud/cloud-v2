package dev.redicloud.database.repository

import dev.redicloud.database.DatabaseConnection
import org.redisson.api.RBucket
import org.redisson.client.codec.BaseCodec

open class DatabaseBucketRepository<T>(
    connection: DatabaseConnection,
    name: String,
    codec: BaseCodec? = null
) : DatabaseRepository<T>(connection, name, codec) {

    suspend fun set(identifier: String, value: T) = getHandle(identifier).set(value)

    suspend fun get(identifier: String): T? = getHandle(identifier).get()

    suspend fun delete(identifier: String): Boolean = getHandle(identifier).delete()

    suspend fun getAll(): List<T> =
        connection.getClient().keys.getKeysByPattern("$name:*").mapNotNull { get(it) }

    suspend fun exists(identifier: String): Boolean = getHandle(identifier).isExists

    fun getHandle(identifier: String, customIdentifier: Boolean = false): RBucket<T> {
        if (!connection.isConnected()) throw IllegalStateException("Not connected to database")
        val databaseIdentifier = if (customIdentifier) identifier else toDatabaseIdentifier(identifier)
        return if (codec != null) connection.getClient().getBucket(databaseIdentifier, codec)
        else connection.getClient().getBucket(databaseIdentifier)
    }

    fun <X> getUnsafeHandle(identifier: String, customIdentifier: Boolean): RBucket<X> {
        if (!connection.isConnected()) throw IllegalStateException("Not connected to database")
        val databaseIdentifier = if (customIdentifier) identifier else toDatabaseIdentifier(identifier)
        return if (codec != null) connection.getClient().getBucket(databaseIdentifier, codec)
        else connection.getClient().getBucket(databaseIdentifier)
    }

}