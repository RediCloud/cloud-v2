package dev.redicloud.database.repository

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.gson.gsonInterfaceFactory
import org.redisson.api.RBucket
import org.redisson.client.codec.BaseCodec
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.isSubclassOf

open class DatabaseBucketRepository<I : Any, K: Any>(
    connection: DatabaseConnection,
    name: String,
    codec: BaseCodec? = null,
    private val interfaceClass: KClass<I>,
    private val implementationClass: KClass<K>
) : DatabaseRepository<K>(connection, name, codec) {

    init {
        if (!implementationClass.isSubclassOf(interfaceClass)) {
            throw IllegalArgumentException("Implementation class must be a subclass of interface class ($implementationClass is not a subclass of $interfaceClass)")
        }
        gsonInterfaceFactory.register(interfaceClass, implementationClass)
    }

    private val unsafe get() = UnsafeDatabaseBucketRepository(this)
    fun unsafe() = unsafe

    protected open suspend fun set(identifier: String, value: I): K {
        if (!implementationClass.isInstance(value)) throw IllegalStateException("${value::class.qualifiedName} is not of type ${implementationClass.qualifiedName}")
        getHandle(identifier).set(value)
        return implementationClass.cast(value)
    }

    protected open suspend fun get(identifier: String): K? {
        val v = getHandle(identifier).get() ?: return null
        if (!implementationClass.isInstance(v)) throw IllegalStateException("${v::class.qualifiedName} is not of type ${implementationClass.qualifiedName}")
        return implementationClass.cast(v)
    }

    protected open suspend fun delete(identifier: String): Boolean = getHandle(identifier).delete()

    protected open suspend fun getAll(customPattern: String? = null): List<K> =
        connection.getClient().keys.getKeysByPattern(customPattern ?: "$name:*")
            .mapNotNull { getUnsafeHandle<K>(it, true).get() }

    protected open suspend fun exists(identifier: String): Boolean = getHandle(identifier).isExists

    private fun getHandle(identifier: String, customIdentifier: Boolean = false): RBucket<I> {
        if (!connection.isConnected()) throw IllegalStateException("Not connected to database")
        val databaseIdentifier = if (customIdentifier) identifier else toDatabaseIdentifier(identifier)
        return if (codec != null) connection.getClient().getBucket(databaseIdentifier, codec)
        else connection.getClient().getBucket(databaseIdentifier)
    }

    private fun <X> getUnsafeHandle(identifier: String, customIdentifier: Boolean): RBucket<X> {
        if (!connection.isConnected()) throw IllegalStateException("Not connected to database")
        val databaseIdentifier = if (customIdentifier) identifier else toDatabaseIdentifier(identifier)
        return if (codec != null) connection.getClient().getBucket(databaseIdentifier, codec)
        else connection.getClient().getBucket(databaseIdentifier)
    }

    class UnsafeDatabaseBucketRepository<I : Any, K: Any>(
        val repository: DatabaseBucketRepository<I, K>
    ) {
        suspend fun set(identifier: String, value: I) = repository.set(identifier, value)
        suspend fun get(identifier: String): K? = repository.get(identifier)
        suspend fun delete(identifier: String): Boolean = repository.delete(identifier)
        suspend fun getAll(customPattern: String? = null): List<K> = repository.getAll(customPattern)
        suspend fun exists(identifier: String): Boolean = repository.exists(identifier)
        fun getHandle(identifier: String, customIdentifier: Boolean = false): RBucket<I> = repository.getHandle(identifier, customIdentifier)
        fun getUnsafeHandle(identifier: String, customIdentifier: Boolean = false): RBucket<K> = repository.getUnsafeHandle(identifier, customIdentifier)
    }

}