package dev.redicloud.cache.packets

import com.google.gson.annotations.Expose
import dev.redicloud.packets.AbstractPacket
import dev.redicloud.cache.ClusterCache
import dev.redicloud.cache.IClusterCacheObject

abstract class CachePacket(
    val cacheName: String
): AbstractPacket() {

    @Expose(deserialize = false, serialize = false)
    var cache: ClusterCache<out IClusterCacheObject>? = null

    override fun received() {
        if (cache != null) {
            cache = ClusterCache.CACHES[cacheName]
        }
    }

}