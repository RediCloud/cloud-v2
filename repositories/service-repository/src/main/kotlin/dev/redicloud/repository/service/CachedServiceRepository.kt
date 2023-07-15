package dev.redicloud.repository.service

import dev.redicloud.cache.ClusterCache
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlin.reflect.KClass
import kotlin.time.Duration

abstract class CachedServiceRepository<T : CloudService>(
    databaseConnection: DatabaseConnection,
    serviceId: ServiceId,
    serviceType: ServiceType,
    packetManager: PacketManager,
    cacheClasses: Array<KClass<out T>>,
    cacheDuration: Duration,
    vararg cacheTypes: ServiceType
) : ServiceRepository<T>(
    databaseConnection,
    serviceId,
    serviceType,
    packetManager
) {

    private val caches = HashMap<KClass<out T>, ClusterCache<out T>>()

    init {
        cacheClasses.forEach { cacheClass ->
            caches[cacheClass] = ClusterCache(name, connection.serviceId, cacheClass, cacheDuration, packetManager, *cacheTypes)
        }
    }

    override suspend fun get(identifier: String): T? {
        val cache = caches.values.firstOrNull { it.isCached(identifier) }
        if (cache != null) return cache.get(identifier)
        val result = super.get(identifier)
        if (result != null) caches[result::class]?.updateCache(identifier, result)
        return result
    }

    override suspend fun set(identifier: String, value: T) {
        caches[value::class]?.updateCache(identifier, value)
        super.set(identifier, value)
    }

    override suspend fun delete(identifier: String): Boolean {
        caches.values.forEach {
            it.updateCache(identifier, null)
        }
        return super.delete(identifier)
    }

    override suspend fun exists(identifier: String): Boolean {
        if (caches.values.any { it.isCached(identifier) }) return true
        return super.exists(identifier)
    }

    override suspend fun getAll(customPattern: String?): List<T> {
        val keyPattern = customPattern ?: "$name:*"
        val keys = connection.getClient().getKeys().getKeysByPattern(keyPattern)
        val new = mutableListOf<String>()
        keys.forEach {
            if (caches.values.any { cache -> cache.isCached(it) }) return@forEach
            val identifier = it.substringAfter("$name:")
            new.add(identifier)
        }
        val toUpdate = mutableMapOf<String, T>()
        new.forEach {
            val result = super.get(it)
            if (result != null) toUpdate[it] = result
        }
        caches.values.forEach { cache ->
            val update = toUpdate.filter { cache.cacheClass.isInstance(it.value) }
            cache.updateCache(update)
        }
        return caches.values.flatMap { it.getCache().values }
    }

}