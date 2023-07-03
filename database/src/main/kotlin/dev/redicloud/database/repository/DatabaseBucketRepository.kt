package dev.redicloud.database.repository

import dev.redicloud.database.DatabaseConnection
import org.redisson.api.RBucket
import org.redisson.client.codec.BaseCodec

open class DatabaseBucketRepository<T>(
    connection: DatabaseConnection,
    name: String,
    codec: BaseCodec? = null
) : DatabaseRepository<T>(connection, name, codec) {

    protected suspend fun set(identifier: String, value: T) = getHandle(identifier).set(value)

    protected suspend fun get(identifier: String): T? = getHandle(identifier).get()

    protected suspend fun delete(identifier: String): Boolean = getHandle(identifier).delete()

    protected suspend fun getAll(customPattern: String? = null): List<T> =
        connection.getClient().keys.getKeysByPattern(customPattern ?: "$name:*")
            .mapNotNull { getUnsafeHandle<T>(it, true).get() }

    protected suspend fun exists(identifier: String): Boolean = getHandle(identifier).isExists

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