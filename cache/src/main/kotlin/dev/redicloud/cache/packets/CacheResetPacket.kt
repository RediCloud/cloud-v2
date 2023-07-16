package dev.redicloud.cache.packets

import dev.redicloud.api.packets.IPacketManager

class CacheResetPacket(
    name: String
) : CachePacket(name) {

        override fun received(manager: IPacketManager) {
            super.received(manager)
            if (cache == null) return
            cache!!.resetCache()
        }

}