package dev.redicloud.packets

import com.google.gson.Gson
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.utils.ServiceId
import dev.redicloud.utils.ServiceType
import org.redisson.api.RTopic
import org.redisson.api.listener.MessageListener

class PacketManager(private val databaseConnection: DatabaseConnection, private val serviceId: ServiceId) {

    private val registeredPackets = mutableListOf<AbstractPacket>()
    private val serviceTopic: RTopic
    private val broadcastTopic: RTopic
    private val typedTopics: MutableMap<ServiceType, RTopic> = mutableMapOf()
    val gson = Gson()

    init {
        if (!databaseConnection.isConnected()) throw IllegalStateException("Database connection is not connected!")

        serviceTopic = databaseConnection.client!!.getTopic(serviceId.toName())
        broadcastTopic = databaseConnection.client!!.getTopic("service")
        ServiceType.values().forEach {
            typedTopics[it] = databaseConnection.client!!.getTopic(it.name.lowercase())
        }

        val messageListener = MessageListener<PackedPacket> { _, messageData ->
            val data = messageData.data
            val p = registeredPackets.firstOrNull { it::class.java.name == messageData.clazz }
                ?: return@MessageListener
            val packet = gson.fromJson(data, p::class.java)
            //TODO: call
        }
        serviceTopic.addListener(PackedPacket::class.java, messageListener)
        broadcastTopic.addListener(PackedPacket::class.java, messageListener)
        typedTopics.forEach { (_, topic) ->
            topic.addListener(PackedPacket::class.java, messageListener)
        }
    }

    fun registerPacket(packet: AbstractPacket) {
        registeredPackets.add(packet)
    }

    fun unregisterPacket(packet: AbstractPacket) {
        registeredPackets.remove(packet)
    }

    suspend fun publishPacket(packet: AbstractPacket, vararg receivers: ServiceId) {
        packet.sender = serviceId
        val packedPacket = PackedPacket(gson.toJson(packet), packet::class.java.name)
        receivers.forEach {
            val targetTopic = databaseConnection.client!!.getTopic(it.toName())
            targetTopic.publish(packedPacket)
        }
    }

    suspend fun publishAll(packet: AbstractPacket) {
        packet.sender = serviceId
        val packedPacket = PackedPacket(gson.toJson(packet), packet::class.java.name)
    }

    suspend fun publish(packet: AbstractPacket, vararg serviceTypes: ServiceType) {
        packet.sender = serviceId
        val packedPacket = PackedPacket(gson.toJson(packet), packet::class.java.name)
        serviceTypes.forEach { typedTopics[it]?.publish(packedPacket) }
    }

}