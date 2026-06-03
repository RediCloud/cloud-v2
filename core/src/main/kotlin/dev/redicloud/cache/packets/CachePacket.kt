package dev.redicloud.cache.packets

import com.google.gson.annotations.Expose
import dev.redicloud.api.cache.IClusterCacheObject
import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.cache.ClusterCache

abstract class CachePacket(
    val cacheName: String
) : AbstractPacket() {

    @Expose(deserialize = false, serialize = false)
    var cache: ClusterCache<out IClusterCacheObject>? = null

    override suspend fun received(manager: IPacketManager) {
        super.received(manager)
        cache = ClusterCache.CACHES[cacheName]
    }
}
