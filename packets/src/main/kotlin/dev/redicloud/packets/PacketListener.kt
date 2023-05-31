package dev.redicloud.packets

import kotlin.reflect.KClass

class PacketListener<T : AbstractPacket>(val packetClazz: KClass<T>, private val handle: (T) -> Unit) {

    fun listener(packet: T) = handle(packet)

}