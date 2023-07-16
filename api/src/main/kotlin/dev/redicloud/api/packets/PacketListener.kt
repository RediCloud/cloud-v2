package dev.redicloud.api.packets

import kotlin.reflect.KClass

open class PacketListener<T : AbstractPacket>(val packetClazz: KClass<T>, private val handle: (T) -> Unit) {

    fun listener(packet: T) = handle(packet)

}