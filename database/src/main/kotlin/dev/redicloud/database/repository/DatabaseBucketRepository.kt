package dev.redicloud.database.repository

import dev.redicloud.api.database.IDatabaseConnection
import dev.redicloud.api.database.grid.bucket.IDataBucket
import dev.redicloud.utils.gson.gsonInterfaceFactory
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.isSubclassOf

open class DatabaseBucketRepository<I : Any, K: Any>(
    connection: IDatabaseConnection,
    name: String,
    private val interfaceClass: KClass<I>,
    private val implementationClass: KClass<K>
) : DatabaseRepository<K>(connection, name) {

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
        connection.getKeysByPattern(customPattern ?: "$name:*")
            .mapNotNull { getUnsafeHandle<K>(it, true).get() }

    protected open suspend fun exists(identifier: String): Boolean = getHandle(identifier).exists

    private fun getHandle(identifier: String, customIdentifier: Boolean = false): IDataBucket<I> {
        if (!connection.connected) throw IllegalStateException("Not connected to database")
        val databaseIdentifier = "cloud:" + (if (customIdentifier) identifier else toDatabaseIdentifier(identifier))
        return connection.getBucket(databaseIdentifier)
    }

    private fun <X> getUnsafeHandle(identifier: String, customIdentifier: Boolean): IDataBucket<X> {
        if (!connection.connected) throw IllegalStateException("Not connected to database")
        val databaseIdentifier = "cloud:" + (if (customIdentifier) identifier else toDatabaseIdentifier(identifier))
        return connection.getBucket(databaseIdentifier)
    }

    class UnsafeDatabaseBucketRepository<I : Any, K: Any>(
        val repository: DatabaseBucketRepository<I, K>
    ) {
        suspend fun set(identifier: String, value: I) = repository.set(identifier, value)
        suspend fun get(identifier: String): K? = repository.get(identifier)
        suspend fun delete(identifier: String): Boolean = repository.delete(identifier)
        suspend fun getAll(customPattern: String? = null): List<K> = repository.getAll(customPattern)
        suspend fun exists(identifier: String): Boolean = repository.exists(identifier)
        fun getHandle(identifier: String, customIdentifier: Boolean = false): IDataBucket<I> = repository.getHandle(identifier, customIdentifier)
        fun getUnsafeHandle(identifier: String, customIdentifier: Boolean = false): IDataBucket<K> = repository.getUnsafeHandle(identifier, customIdentifier)
    }

}