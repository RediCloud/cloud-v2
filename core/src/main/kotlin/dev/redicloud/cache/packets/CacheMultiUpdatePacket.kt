package dev.redicloud.cache.packets

import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.utils.gson.gson

/**
 * Packet that updates multiple entries in a cluster cache in a single operation.
 *
 * When received, deserializes each value from JSON and applies all updates to the local cache.
 * Null values in [toUpdate] remove the corresponding entries.
 *
 * @property toUpdate a map of cache keys to their JSON-serialized new values (null to remove)
 */
class CacheMultiUpdatePacket(
    name: String,
    val toUpdate: Map<String, String?>
) : CachePacket(name) {

    override suspend fun received(manager: IPacketManager) {
        super.received(manager)
        if (cache == null) return
        toUpdate.forEach { (key, valueJson) ->
            if (valueJson == null) {
                cache!!.setCached(key, null)
                return@forEach
            }
            val clazz = cache!!.cacheClass
            val value = gson.fromJson(valueJson, clazz.java)
            cache!!.setCached(key, value)
        }
    }
}
