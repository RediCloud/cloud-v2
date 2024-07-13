package dev.redicloud.packets

import com.google.gson.GsonBuilder
import dev.redicloud.api.database.communication.IChannelListener
import dev.redicloud.api.database.communication.ICommunicationChannel
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
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

class PacketManager(
    private val databaseConnection: DatabaseConnection,
    override val serviceId: ServiceId
) : IPacketManager {

    companion object {
        private val LOGGER = LogManager.logger(PacketManager::class.java)
    }

    private var categoryChannelName: String? = null
    private val registeredPackets = mutableListOf<KClass<out AbstractPacket>>()
    private val serviceTopic: ICommunicationChannel
    private val broadcastTopic: ICommunicationChannel
    private var categoryChannel: ICommunicationChannel? = null
    private val typedTopics: MutableMap<ServiceType, ICommunicationChannel> = mutableMapOf()
    private val gson = GsonBuilder().fixKotlinAnnotations().create()
    private val listeners = mutableListOf<PacketListener<out AbstractPacket>>()
    internal val packetResponses = mutableListOf<PacketResponse>()
    internal val packetsOfLast3Seconds = mutableListOf<AbstractPacket>()
    internal val packetScope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
    private val messageListener = createMessageListener()

    init {
        if (!databaseConnection.connected) throw IllegalStateException("Database connection is not connected!")

        serviceTopic = databaseConnection.getCommunicationChannel(serviceId.toName())
        broadcastTopic = databaseConnection.getCommunicationChannel("broadcast")
        ServiceType.entries.forEach {
            typedTopics[it] = databaseConnection.getCommunicationChannel(it.name.lowercase())
        }

        runBlocking {
            serviceTopic.subscribe(PackedPacket::class.java, messageListener)
            broadcastTopic.subscribe(PackedPacket::class.java, messageListener)
            typedTopics.forEach { (_, topic) ->
                topic.subscribe(PackedPacket::class.java, messageListener)
            }
        }
    }

    private fun createMessageListener(): IChannelListener<PackedPacket> {
        return object : IChannelListener<PackedPacket> {
            override fun onMessage(channel: String, message: PackedPacket) {
                val data = message.data
                val packetClazz = registeredPackets.firstOrNull { it.qualifiedName == message.clazz }
                    ?: return kotlin.run { LOGGER.fine("Received packet with unknown class ${message.clazz} in channel $channel") }
                val packet = gson.fromJson(data, packetClazz.java)
                if (!packet.allowLocalReceiver && packet.sender == serviceId) return
                LOGGER.finest("Received packet ${packetClazz.simpleName} in channel $channel")
                packet.received(this@PacketManager)
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
        }
    }

    suspend fun disconnect() {
        serviceTopic.unsubscribeAll()
        broadcastTopic.unsubscribeAll()
        typedTopics.forEach { (_, topic) -> topic.unsubscribeAll() }
        categoryChannel?.unsubscribeAll()
        packetScope.cancel()
    }

    suspend fun registerCategoryChannel(name: String) {
        if (this.categoryChannelName != null) throw IllegalStateException("Category channel is already registered!")
        this.categoryChannelName = name

        categoryChannel?.subscribe(PackedPacket::class.java, messageListener)
        if (categoryChannelName != null) {
            this.categoryChannel = databaseConnection.getCommunicationChannel(categoryChannelName!!)
        } else {
            this.categoryChannel = null
        }
    }

    override fun isConnected(): Boolean {
        return serviceTopic.subscribtionCount != 0
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
            val targetTopic = databaseConnection.getCommunicationChannel(it.toName())
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
        databaseConnection.getCommunicationChannel(categoryName).publish(packedPacket)
        return PacketResponse(this, packet)
    }


}