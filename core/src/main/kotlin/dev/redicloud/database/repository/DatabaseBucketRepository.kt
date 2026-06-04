package dev.redicloud.database.repository

import dev.redicloud.api.database.IDatabaseConnection
import dev.redicloud.api.database.grid.bucket.IDataBucket
import dev.redicloud.utils.gson.gsonInterfaceFactory
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.isSubclassOf

/**
 * A repository that stores entities as Redis buckets, mapping an interface type [I]
 * to its implementation type [K].
 *
 * Automatically registers the interface-to-implementation mapping with the Gson
 * interface factory for proper deserialization. All keys are prefixed with `cloud:<name>:`.
 *
 * @param I the interface type used for bucket storage
 * @param K the implementation type (must be a subclass of [I])
 * @param connection the active database connection
 * @param name the repository key prefix
 * @param interfaceClass the KClass of the interface type
 * @param implementationClass the KClass of the implementation type
 */
open class DatabaseBucketRepository<I : Any, K : Any>(
    connection: IDatabaseConnection,
    name: String,
    private val interfaceClass: KClass<I>,
    private val implementationClass: KClass<K>
) : DatabaseRepository<K>(connection, name) {

    init {
        require(implementationClass.isSubclassOf(interfaceClass)) {
            "Implementation class must be a subclass of interface class " +
                "($implementationClass is not a subclass of $interfaceClass)"
        }
        gsonInterfaceFactory.register(interfaceClass, implementationClass)
    }

    private val unsafe get() = UnsafeDatabaseBucketRepository(this)

    /**
     * Returns an unsafe accessor that exposes all CRUD operations publicly.
     *
     * @return an [UnsafeDatabaseBucketRepository] wrapping this repository
     */
    fun unsafe() = unsafe

    protected open suspend fun set(identifier: String, value: I): K {
        check(implementationClass.isInstance(value)) {
            "${value::class.qualifiedName} is not of type ${implementationClass.qualifiedName}"
        }
        getHandle(identifier).set(value)
        return implementationClass.cast(value)
    }

    protected open suspend fun get(identifier: String): K? {
        val v = getHandle(identifier).get() ?: return null
        check(implementationClass.isInstance(v)) {
            "${v::class.qualifiedName} is not of type ${implementationClass.qualifiedName}"
        }
        return implementationClass.cast(v)
    }

    protected open suspend fun delete(identifier: String): Boolean = getHandle(identifier).delete()

    protected open suspend fun getAll(customPattern: String? = null): List<K> =
        connection.getKeysByPattern(customPattern ?: "$name:*")
            .mapNotNull { getUnsafeHandle<K>(it, true).get() }

    protected open suspend fun exists(identifier: String): Boolean = getHandle(identifier).exists

    private fun getHandle(identifier: String, customIdentifier: Boolean = false): IDataBucket<I> {
        check(connection.connected) { "Not connected to database" }
        val databaseIdentifier = "cloud:" + (if (customIdentifier) identifier else toDatabaseIdentifier(identifier))
        return connection.getBucket(databaseIdentifier)
    }

    private fun <X> getUnsafeHandle(identifier: String, customIdentifier: Boolean): IDataBucket<X> {
        check(connection.connected) { "Not connected to database" }
        val databaseIdentifier = "cloud:" + (if (customIdentifier) identifier else toDatabaseIdentifier(identifier))
        return connection.getBucket(databaseIdentifier)
    }

    /**
     * Publicly exposes all CRUD operations of a [DatabaseBucketRepository].
     *
     * Use with caution since it bypasses any access restrictions defined
     * by the enclosing repository's protected methods.
     *
     * @param I the interface type
     * @param K the implementation type
     * @param repository the underlying repository to delegate to
     */
    class UnsafeDatabaseBucketRepository<I : Any, K : Any>(
        val repository: DatabaseBucketRepository<I, K>
    ) {
        /** Stores the given [value] under the specified [identifier]. */
        suspend fun set(identifier: String, value: I) = repository.set(identifier, value)

        /** Retrieves the value for the given [identifier], or null if not found. */
        suspend fun get(identifier: String): K? = repository.get(identifier)

        /** Deletes the entry for the given [identifier]. Returns true if it existed. */
        suspend fun delete(identifier: String): Boolean = repository.delete(identifier)

        /** Returns all entries, optionally filtered by a custom Redis key [customPattern]. */
        suspend fun getAll(customPattern: String? = null): List<K> = repository.getAll(customPattern)

        /** Checks whether an entry with the given [identifier] exists. */
        suspend fun exists(identifier: String): Boolean = repository.exists(identifier)

        /**
         * Returns the raw [IDataBucket] handle for the given [identifier].
         *
         * @param identifier the entity identifier
         * @param customIdentifier if true, uses the identifier as-is without prefixing
         * @return the data bucket handle typed to the interface [I]
         */
        fun getHandle(
            identifier: String,
            customIdentifier: Boolean = false
        ): IDataBucket<I> = repository.getHandle(identifier, customIdentifier)

        /**
         * Returns an unsafe [IDataBucket] handle with the implementation type [K].
         *
         * @param identifier the entity identifier
         * @param customIdentifier if true, uses the identifier as-is without prefixing
         * @return the data bucket handle typed to the implementation [K]
         */
        fun getUnsafeHandle(
            identifier: String,
            customIdentifier: Boolean = false
        ): IDataBucket<K> = repository.getUnsafeHandle(identifier, customIdentifier)
    }
}
