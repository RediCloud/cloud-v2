package dev.redicloud.cache.packets

import com.google.gson.annotations.Expose
import dev.redicloud.api.cache.IClusterCacheObject
import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.cache.ClusterCache

/**
 * Base packet for cluster cache operations.
 *
 * Resolves the target [ClusterCache] by [cacheName] upon reception.
 * Subclasses implement the specific cache mutation logic.
 *
 * @property cacheName the name of the target cluster cache
 */
abstract class CachePacket(
    val cacheName: String
) : AbstractPacket() {

    /** The resolved cache instance, populated on [received]. Null if no matching cache is registered. */
    @Expose(deserialize = false, serialize = false)
    var cache: ClusterCache<out IClusterCacheObject>? = null

    override suspend fun received(manager: IPacketManager) {
        super.received(manager)
        cache = ClusterCache.CACHES[cacheName]
    }
}
