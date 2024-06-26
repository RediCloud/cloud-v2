package dev.redicloud.repository.cache

import dev.redicloud.api.database.IDatabaseConnection
import dev.redicloud.cache.ClusterCache
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.packets.PacketManager
import dev.redicloud.api.service.ServiceType
import kotlin.reflect.KClass
import kotlin.time.Duration

open class CachedDatabaseBucketRepository<I : Any, K : Any> (
    connection: IDatabaseConnection,
    name: String,
    interfaceClass: KClass<I>,
    cacheClass: KClass<K>,
    cacheDuration: Duration,
    packetManager: PacketManager,
    vararg cacheTypes: ServiceType
) : DatabaseBucketRepository<I, K>(
    connection,
    name,
    interfaceClass,
    cacheClass
) {

    protected val cache = ClusterCache(name, connection.serviceId, cacheClass, cacheDuration, packetManager, *cacheTypes)

    override suspend fun get(identifier: String): K? {
        if (cache.isCached(identifier)) return cache.get(identifier)
        val result = super.get(identifier)
        if (result != null) cache.updateCache(identifier, result)
        return result
    }

    override suspend fun set(identifier: String, value: I): K {
        cache.updateCache(identifier, value)
        return super.set(identifier, value)
    }

    override suspend fun delete(identifier: String): Boolean {
        if (cache.isCached(identifier)) cache.updateCache(identifier, null)
        return super.delete(identifier)
    }

    override suspend fun exists(identifier: String): Boolean {
        if (cache.isCached(identifier)) return true
        return super.exists(identifier)
    }

    override suspend fun getAll(customPattern: String?): List<K> {
        val keyPattern = customPattern ?: "cloud:$name:*"
        val keys = connection.getKeysByPattern(keyPattern)
        val new = mutableListOf<String>()
        keys.forEach {
            val identifier = it.substringAfter("$name:")
            if (cache.isCached(identifier)) return@forEach
            new.add(identifier)
        }
        val toUpdate = mutableMapOf<String, K>()
        new.forEach {
            val result = super.get(it)
            if (result != null) toUpdate[it] = result
        }
        cache.updateCache(toUpdate)
        return cache.getCache().values.toList() + toUpdate.values.toList()
    }

}