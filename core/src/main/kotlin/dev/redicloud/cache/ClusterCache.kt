package dev.redicloud.cache

import dev.redicloud.api.cache.IClusterCacheObject
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.cache.packets.CacheMultiUpdatePacket
import dev.redicloud.cache.packets.CacheResetPacket
import dev.redicloud.cache.packets.CacheUpdatePacket
import dev.redicloud.utils.gson.gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.time.Duration

/**
 * Cluster-wide in-memory cache with time-based expiration and cross-node synchronization.
 *
 * Cached values are kept locally in a [ConcurrentHashMap] and changes are broadcast
 * to other nodes via [CacheUpdatePacket], [CacheMultiUpdatePacket], and [CacheResetPacket].
 * Only service types listed in [serviceTypes] will participate in caching.
 *
 * @param V the cached value type, must implement [IClusterCacheObject]
 * @param name a unique name identifying this cache across the cluster
 * @param serviceId the identity of the owning service
 * @param cacheClass the KClass of the cached value type
 * @param cacheDuration how long cached entries remain valid
 * @param packetManager the packet manager used to broadcast cache updates
 * @param scope the coroutine scope for launching broadcast operations
 * @param serviceTypes the service types that should participate in this cache
 */
class ClusterCache<V : Any>(
    val name: String,
    val serviceId: ServiceId,
    val cacheClass: KClass<V>,
    val cacheDuration: Duration,
    private val packetManager: IPacketManager,
    private val scope: CoroutineScope,
    vararg val serviceTypes: ServiceType,
) {

    init {
        require(cacheClass.isSubclassOf(IClusterCacheObject::class)) {
            "Cache class must implement IClusterCacheObject (class: ${cacheClass.qualifiedName})"
        }
        if (!packetManager.isPacketRegistered(CacheUpdatePacket::class)) {
            packetManager.registerPacket(CacheUpdatePacket::class)
        }
        if (!packetManager.isPacketRegistered(CacheResetPacket::class)) {
            packetManager.registerPacket(CacheResetPacket::class)
        }
        if (!packetManager.isPacketRegistered(CacheMultiUpdatePacket::class)) {
            packetManager.registerPacket(CacheMultiUpdatePacket::class)
        }
        CACHES[name] = this as ClusterCache<out IClusterCacheObject>
    }

    companion object {
        /** Global registry of all active cluster caches, keyed by cache name. */
        val CACHES = mutableMapOf<String, ClusterCache<out IClusterCacheObject>>()
    }

    private val cache = ConcurrentHashMap<String, Pair<Long, V>>()

    /**
     * Retrieves a cached value by key.
     *
     * @param key the cache key
     * @return the cached value, or null if not cached or service type doesn't match
     */
    fun get(key: String): V? {
        if (serviceTypes.none { it == serviceId.type }) return null
        return cache[key]?.second
    }

    /**
     * Retrieves a cached value by key, populating the cache via [block] on a miss.
     *
     * @param key the cache key
     * @param block supplier invoked to compute the value if not cached
     * @return the cached or freshly computed value, or null
     */
    fun get(key: String, block: () -> V?): V? {
        if (serviceTypes.none { it == serviceId.type }) return null
        if (!isCached(key)) {
            setCached(key, block())
        }
        return get(key)
    }

    /**
     * Updates a single cache entry and broadcasts the change to other nodes.
     *
     * @param key the cache key
     * @param value the new value, or null to remove the entry
     */
    fun updateCache(key: String, value: Any?) {
        if (value == null) {
            if (!isCached(key)) return
            setCached(key, value)
            scope.launch {
                serviceTypes.forEach {
                    packetManager.publish(CacheUpdatePacket(name, key, null), it)
                }
            }
            return
        }
        setCached(key, value)
        scope.launch {
            serviceTypes.forEach {
                packetManager.publish(CacheUpdatePacket(name, key, gson.toJson(value)), it)
            }
        }
    }

    /**
     * Updates multiple cache entries at once and broadcasts the changes to other nodes.
     *
     * @param toUpdate a map of cache keys to their new values (null to remove)
     */
    fun updateCache(toUpdate: Map<String, Any?>) {
        if (toUpdate.isEmpty()) return
        toUpdate.forEach {
            setCached(it.key, it.value)
        }
        scope.launch {
            val map = toUpdate.mapValues { gson.toJson(it.value) }
            serviceTypes.forEach {
                packetManager.publish(CacheMultiUpdatePacket(name, map), it)
            }
        }
    }

    /** Clears all local cache entries and broadcasts a reset to other nodes. */
    fun clearCache() {
        cache.clear()
        scope.launch {
            serviceTypes.forEach {
                packetManager.publish(CacheResetPacket(name), it)
            }
        }
    }

    /**
     * Returns a snapshot of all cached entries as a simple key-to-value map.
     *
     * @return an immutable view of the current cache contents
     */
    fun getCache(): Map<String, V> {
        return cache.mapValues { it.value.second }
    }

    /**
     * Checks whether a key is cached, optionally validating expiration.
     *
     * @param key the cache key
     * @param validCheck if true (default), also checks that the entry has not expired
     * @return true if the key is present (and valid, if [validCheck] is true)
     */
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
        require(value::class == cacheClass) { "Value is not of type ${cacheClass.simpleName}" }
        cache[key] = System.currentTimeMillis() to value as V
    }

    internal fun isCacheValid(key: String): Boolean {
        return isCached(key) &&
            System.currentTimeMillis() - cache[key]!!.first < cacheDuration.inWholeMilliseconds
    }
}
