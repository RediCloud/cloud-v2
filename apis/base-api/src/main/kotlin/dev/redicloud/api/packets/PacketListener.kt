package dev.redicloud.api.packets

import kotlin.reflect.KClass

open class PacketListener<T : AbstractPacket>(val packetClazz: KClass<T>, private val handle: suspend (T) -> Unit) {

    val classLoader = this::class.java.classLoader

    suspend fun listener(packet: T) = handle(packet)
}
