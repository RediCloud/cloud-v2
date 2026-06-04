package dev.redicloud.cache.packets

import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.utils.gson.gson

/**
 * Packet that updates a single entry in a cluster cache.
 *
 * When received, deserializes the value from JSON and updates the local cache.
 * A null [valueJson] removes the entry from the cache.
 *
 * @property key the cache key to update
 * @property valueJson the JSON-serialized new value, or null to remove the entry
 */
class CacheUpdatePacket(
    name: String,
    val key: String,
    val valueJson: String?
) : CachePacket(name) {

    override suspend fun received(manager: IPacketManager) {
        super.received(manager)
        if (cache == null) return
        if (valueJson == null) {
            cache!!.setCached(key, null)
            return
        }
        val clazz = cache!!.cacheClass
        val value = gson.fromJson(valueJson, clazz.java)
        cache!!.setCached(key, value)
    }
}
