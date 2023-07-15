package dev.redicloud.cache.packets

import dev.redicloud.utils.gson.gson

class CacheMultiUpdatePacket(
    name: String,
    val toUpdate: Map<String, String?>
) : CachePacket(name) {

    override fun received() {
        super.received()
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