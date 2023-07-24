package dev.redicloud.packets

import com.google.gson.GsonBuilder
import dev.redicloud.api.events.InlineEventCaller
import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.packets.IPacketResponse
import dev.redicloud.api.packets.PacketListener
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.gson.fixKotlinAnnotations
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.utils.coroutineExceptionHandler
import kotlinx.coroutines.*
import org.redisson.api.RReliableTopic
import org.redisson.api.RTopic
import org.redisson.api.listener.MessageListener
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

class PacketManager(
    private val databaseConnection: DatabaseConnection,
    override val serviceId: ServiceId,
    private val categoryChannelName: String? = null
) : IPacketManager {

    companion object {
        private val LOGGER = LogManager.logger(PacketManager::class.java)
    }

    private val registeredPackets = mutableListOf<KClass<out AbstractPacket>>()
    private val serviceTopic: RTopic
    private val broadcastTopic: RTopic
    private val categoryChannel: RTopic?
    private val typedTopics: MutableMap<ServiceType, RTopic> = mutableMapOf()
    private val gson = GsonBuilder().fixKotlinAnnotations().create()
    private val listeners = mutableListOf<PacketListener<out AbstractPacket>>()
    internal val packetResponses = mutableListOf<PacketResponse>()
    internal val packetsOfLast3Seconds = mutableListOf<AbstractPacket>()
    internal val packetScope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

    init {
        if (!databaseConnection.isConnected()) throw IllegalStateException("Database connection is not connected!")

        serviceTopic = databaseConnection.getClient().getTopic(serviceId.toName())
        broadcastTopic = databaseConnection.getClient().getTopic("broadcast")
        ServiceType.values().forEach {
            typedTopics[it] = databaseConnection.getClient().getTopic(it.name.lowercase())
        }
        if (categoryChannelName != null) {
            this.categoryChannel = databaseConnection.getClient().getTopic(categoryChannelName)
        } else {
            this.categoryChannel = null
        }

        val messageListener = MessageListener<PackedPacket> { channel, messageData ->
            val data = messageData.data
            val p = registeredPackets.firstOrNull { it.qualifiedName == messageData.clazz }
            if (p == null) {
                LOGGER.warning("Received packet in channel $channel but packet is not registered: ${messageData.clazz}")
                return@MessageListener
            }
            val packet = gson.fromJson(data, p.java)
            if (!packet.allowLocalReceiver && packet.sender == serviceId) return@MessageListener
            LOGGER.finest("Received packet ${p.simpleName} in channel $channel")
            packet.received(this)
            packetsOfLast3Seconds.add(packet)
            packetScope.launch {
                delay(3.seconds)
                packetsOfLast3Seconds.remove(packet)
            }
            ArrayList(packetResponses).filterNotNull().forEach {
                try {
                    it.handle(packet)
                }catch (e: Exception) {
                    LOGGER.severe("Error while handling packet response ${packet::class.java.simpleName}!", e)
                }
            }
            listeners.forEach {
                if (packet::class == it.packetClazz) {
                    try {
                        (it as PacketListener<AbstractPacket>).listener(packet)
                    } catch (e: Exception) {
                        LOGGER.severe("Error while handling packet ${packet::class.java.simpleName}!", e)
                    }
                }
            }
        }
        serviceTopic.addListener(PackedPacket::class.java, messageListener)
        broadcastTopic.addListener(PackedPacket::class.java, messageListener)
        typedTopics.forEach { (_, topic) ->
            topic.addListener(PackedPacket::class.java, messageListener)
        }
        categoryChannel?.addListener(PackedPacket::class.java, messageListener)
    }

    fun disconnect() {
        serviceTopic.removeAllListeners()
        broadcastTopic.removeAllListeners()
        typedTopics.forEach { (_, topic) -> topic.removeAllListeners() }
        categoryChannel?.removeAllListeners()
        packetScope.cancel()
    }

    override fun isConnected(): Boolean {
        return serviceTopic.countListeners() != 0
    }

    override fun isPacketRegistered(packetClazz: KClass<out AbstractPacket>): Boolean {
        return registeredPackets.any { it::class == packetClazz }
    }

    override fun registerPacket(packetClazz: KClass<out AbstractPacket>) {
        registeredPackets.add(packetClazz)
    }

    override fun unregisterPacket(packetClazz: KClass<out AbstractPacket>) {
        registeredPackets.remove(packetClazz)
    }

    override fun registerListener(listener: PacketListener<out AbstractPacket>) {
        listeners.add(listener)
    }

    override fun unregisterListener(listener: PacketListener<out AbstractPacket>) {
        listeners.remove(listener)
    }

    fun unregister(classLoader: ClassLoader) {
        listeners.removeIf { it.javaClass.classLoader == classLoader }
        registeredPackets.removeIf { it.java.classLoader == classLoader }
    }

    override suspend fun publish(packet: AbstractPacket, vararg receivers: ServiceId): PacketResponse {
        packet.sender = serviceId
        val packedPacket = PackedPacket(gson.toJson(packet), packet::class.java.name)
        receivers.forEach {
            val targetTopic = databaseConnection.getClient().getTopic(it.toName())
            targetTopic.publish(packedPacket)
        }
        return PacketResponse(this, packet)
    }

    override suspend fun publishBroadcast(packet: AbstractPacket): PacketResponse {
        packet.sender = serviceId
        val packedPacket = PackedPacket(gson.toJson(packet), packet::class.java.name)
        broadcastTopic.publish(packedPacket)
        return PacketResponse(this, packet)
    }

    override suspend fun publish(packet: AbstractPacket, vararg serviceTypes: ServiceType): PacketResponse {
        packet.sender = serviceId
        val packedPacket = PackedPacket(gson.toJson(packet), packet::class.java.name)
        serviceTypes.forEach { typedTopics[it]?.publish(packedPacket) }
        return PacketResponse(this, packet)
    }

    override suspend fun publishToCategory(packet: AbstractPacket, categoryName: String): IPacketResponse {
        packet.sender = serviceId
        val packedPacket = PackedPacket(gson.toJson(packet), packet::class.java.name)
        databaseConnection.getClient().getTopic(categoryName).publish(packedPacket)
        return PacketResponse(this, packet)
    }


}