package dev.redicloud.api.packets

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import kotlin.reflect.KClass

interface IPacketManager {

    val serviceId: ServiceId

    fun isConnected(): Boolean

    fun isPacketRegistered(packetClazz: Class<out AbstractPacket>): Boolean {
        return isPacketRegistered(packetClazz.kotlin)
    }

    fun isPacketRegistered(packetClazz: KClass<out AbstractPacket>): Boolean

    fun registerPacket(packetClazz: Class<out AbstractPacket>) {
        registerPacket(packetClazz.kotlin)
    }

    fun registerPacket(packetClazz: KClass<out AbstractPacket>)

    fun unregisterPacket(packetClazz: Class<out AbstractPacket>) {
        unregisterPacket(packetClazz.kotlin)
    }

    fun unregisterPacket(packetClazz: KClass<out AbstractPacket>)

    fun registerListener(listener: PacketListener<out AbstractPacket>)

    fun unregisterListener(listener: PacketListener<out AbstractPacket>)

    suspend fun publish(packet: AbstractPacket, vararg receivers: ServiceId): IPacketResponse

    suspend fun publishBroadcast(packet: AbstractPacket): IPacketResponse

    suspend fun publish(packet: AbstractPacket, vararg serviceTypes: ServiceType): IPacketResponse

    suspend fun publishToCategory(packet: AbstractPacket, categoryName: String): IPacketResponse

}

inline fun <reified T : AbstractPacket> IPacketManager.listen(noinline handler: (T) -> Unit): PacketListener<T> {
    val listener = PacketListener(T::class, handler)
    registerListener(listener)
    return listener
}

fun <T : AbstractPacket> IPacketManager.listen(clazz: KClass<T>, handler: (T) -> Unit): PacketListener<T> {
    val listener = PacketListener(clazz, handler)
    registerListener(listener)
    return listener
}