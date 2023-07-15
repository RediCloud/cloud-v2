package dev.redicloud.repository.cache

import dev.redicloud.cache.ClusterCache
import dev.redicloud.cache.IClusterCacheObject
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.service.ServiceType
import org.redisson.client.codec.BaseCodec
import kotlin.reflect.KClass
import kotlin.time.Duration

open class CachedDatabaseBucketRepository<T : IClusterCacheObject>(
    connection: DatabaseConnection,
    name: String,
    codec: BaseCodec? = null,
    cacheClass: KClass<T>,
    cacheDuration: Duration,
    packetManager: PacketManager,
    vararg cacheTypes: ServiceType
) : DatabaseBucketRepository<T>(
    connection,
    name,
    codec
) {

    protected val cache = ClusterCache(name, connection.serviceId, cacheClass, cacheDuration, packetManager, *cacheTypes)

    override suspend fun get(identifier: String): T? {
        if (cache.isCached(identifier)) return cache.get(identifier)
        val result = super.get(identifier)
        if (result != null) cache.updateCache(identifier, result)
        return result
    }

    override suspend fun set(identifier: String, value: T) {
        cache.updateCache(identifier, value)
        super.set(identifier, value)
    }

    override suspend fun delete(identifier: String): Boolean {
        if (cache.isCached(identifier)) cache.updateCache(identifier, null)
        return super.delete(identifier)
    }

    override suspend fun exists(identifier: String): Boolean {
        if (cache.isCached(identifier)) return true
        return super.exists(identifier)
    }

    override suspend fun getAll(customPattern: String?): List<T> {
        val keyPattern = customPattern ?: "$name:*"
        val keys = connection.getClient().keys.getKeysByPattern(keyPattern)
        val new = mutableListOf<String>()
        keys.forEach {
            if (cache.isCached(it)) return@forEach
            val identifier = it.substringAfter("$name:")
            new.add(identifier)
        }
        val toUpdate = mutableMapOf<String, T>()
        new.forEach {
            val result = super.get(it)
            if (result != null) toUpdate[it] = result
        }
        cache.updateCache(toUpdate)
        return cache.getCache().values.toList() + toUpdate.values.toList()
    }

}