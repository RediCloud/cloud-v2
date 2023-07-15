package dev.redicloud.cache

import dev.redicloud.cache.packets.CacheMultiUpdatePacket
import dev.redicloud.cache.packets.CacheResetPacket
import dev.redicloud.cache.packets.CacheUpdatePacket
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.time.Duration

class ClusterCache<V: IClusterCacheObject>(
    val name: String,
    val serviceId: ServiceId,
    val cacheClass: KClass<V>,
    val cacheDuration: Duration,
    private val packetManager: PacketManager,
    vararg val serviceTypes: ServiceType,
) {

    init {
        if (!packetManager.isPacketRegistered(CacheUpdatePacket::class)) {
            packetManager.registerPacket(CacheUpdatePacket::class)
        }
        if (!packetManager.isPacketRegistered(CacheResetPacket::class)) {
            packetManager.registerPacket(CacheResetPacket::class)
        }
        if (!packetManager.isPacketRegistered(CacheMultiUpdatePacket::class)) {
            packetManager.registerPacket(CacheMultiUpdatePacket::class)
        }
        CACHES[name] = this
    }

    companion object {
        val CACHES = mutableMapOf<String, ClusterCache<out IClusterCacheObject>>()
    }

    private val cache = ConcurrentHashMap<String, Pair<Long, V>>()

    fun get(key: String): V? {
        if (serviceTypes.none { it == serviceId.type }) return null
        return cache[key]?.second
    }

    fun get(key: String, block: () -> V?): V? {
        if (serviceTypes.none { it == serviceId.type }) return null
        if (!isCached(key)) {
            setCached(key, block())
        }
        return get(key)
    }

    fun updateCache(key: String, value: Any?) {
        if (value == null) {
            if (!isCached(key)) return
            setCached(key, value)
            defaultScope.launch {
                serviceTypes.forEach {
                    packetManager.publish(CacheUpdatePacket(name, key, null), it)
                }
            }
            return
        }
        setCached(key, value)
        defaultScope.launch {
            serviceTypes.forEach {
                packetManager.publish(CacheUpdatePacket(name, key, gson.toJson(value)), it)
            }
        }
    }

    fun updateCache(toUpdate: Map<String, Any?>) {
        if (toUpdate.isEmpty()) return
        toUpdate.forEach {
            setCached(it.key, it.value)
        }
        defaultScope.launch {
            val map = toUpdate.mapValues { gson.toJson(it.value) }
            serviceTypes.forEach {
                packetManager.publish(CacheMultiUpdatePacket(name, map), it)
            }
        }
    }

    fun clearCache() {
        cache.clear()
        defaultScope.launch {
            serviceTypes.forEach {
                packetManager.publish(CacheResetPacket(name), it)
            }
        }
    }

    fun getCache(): Map<String, V> {
        return cache.mapValues { it.value.second }
    }

    fun isCached(key: String, validCheck: Boolean = true): Boolean {
        if (serviceTypes.none { it == serviceId.type }) return false
        return if (validCheck) isCacheValid(key) else isCached(key)
    }

    internal fun isCached(key: String): Boolean {
        return cache.containsKey(key)
    }

    internal fun resetCache() {
        cache.clear()
    }

    internal fun setCached(key: String, value: Any?) {
        if (value == null) {
            cache.remove(key)
            return
        }
        if (value::class != cacheClass) throw IllegalArgumentException("Value is not of type ${cacheClass.simpleName}")
        cache[key] = System.currentTimeMillis() to value as V
    }

    internal fun isCacheValid(key: String): Boolean {
        return isCached(key) &&
                System.currentTimeMillis() - cache[key]!!.first < cacheDuration.inWholeMilliseconds
    }

}