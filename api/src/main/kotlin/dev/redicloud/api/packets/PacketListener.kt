package dev.redicloud.api.packets

import kotlin.reflect.KClass

open class PacketListener<T : AbstractPacket>(val packetClazz: KClass<T>, private val handle: (T) -> Unit) {

    val classLoader = this::class.java.classLoader

    fun listener(packet: T) = handle(packet)

}