package dev.redicloud.cache.packets

import dev.redicloud.utils.gson.gson

class CacheUpdatePacket(
    name: String,
    val key: String,
    val valueJson: String?
) : CachePacket(name) {

    override fun received() {
        super.received()
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