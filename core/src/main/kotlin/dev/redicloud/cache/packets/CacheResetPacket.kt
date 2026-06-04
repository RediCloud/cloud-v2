package dev.redicloud.cache.packets

import dev.redicloud.api.packets.IPacketManager

/**
 * Packet that clears all entries in a cluster cache.
 *
 * When received, resets the entire local cache identified by the cache name.
 */
class CacheResetPacket(
    name: String
) : CachePacket(name) {

    override suspend fun received(manager: IPacketManager) {
        super.received(manager)
        if (cache == null) return
        cache!!.resetCache()
    }
}
