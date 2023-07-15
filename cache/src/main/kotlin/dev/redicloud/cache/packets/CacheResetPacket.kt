package dev.redicloud.cache.packets

class CacheResetPacket(
    name: String
) : CachePacket(name) {

        override fun received() {
            super.received()
            if (cache == null) return
            cache!!.resetCache()
        }

}