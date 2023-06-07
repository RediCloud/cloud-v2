package dev.redicloud.packets

import com.google.gson.Gson
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.redisson.api.RTopic
import org.redisson.api.listener.MessageListener
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

class PacketManager(private val databaseConnection: DatabaseConnection, val serviceId: ServiceId) {

    companion object {
        private val LOGGER = LogManager.logger(PacketManager::class.java)
    }

    private val registeredPackets = mutableListOf<AbstractPacket>()
    private val serviceTopic: RTopic
    private val broadcastTopic: RTopic
    private val typedTopics: MutableMap<ServiceType, RTopic> = mutableMapOf()
    val gson = Gson()
    val listeners = mutableListOf<PacketListener<out AbstractPacket>>()
    internal val packetResponses = mutableListOf<PacketResponse>()
    internal val packetsOfLast3Seconds = mutableListOf<AbstractPacket>()

    init {
        if (!databaseConnection.isConnected()) throw IllegalStateException("Database connection is not connected!")

        serviceTopic = databaseConnection.getClient().getTopic(serviceId.toName())
        broadcastTopic = databaseConnection.getClient().getTopic("service")
        ServiceType.values().forEach {
            typedTopics[it] = databaseConnection.getClient().getTopic(it.name.lowercase())
        }

        val messageListener = MessageListener<PackedPacket> { _, messageData ->
            val data = messageData.data
            val p = registeredPackets.firstOrNull { it::class.java.name == messageData.clazz }
                ?: return@MessageListener
            val packet = gson.fromJson(data, p::class.java)
            packet.manager = this
            packet.received()
            packetsOfLast3Seconds.add(packet)
            GlobalScope.launch {
                delay(3.seconds)
                packetsOfLast3Seconds.remove(packet)
            }
            packetResponses.forEach {
                try {
                    it.handle(packet)
                }catch (e: Exception) {
                    LOGGER.severe("Error while handling packet response ${p::class.java.simpleName}!", e)
                }
            }
            listeners.forEach {
                if (it.packetClazz == p::class) {
                    try {
                        (it as PacketListener<AbstractPacket>).listener(packet)
                    } catch (e: Exception) {
                        LOGGER.severe("Error while handling packet ${p::class.java.simpleName}!", e)
                    }
                }
            }
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

    inline fun <reified T : AbstractPacket> listen(noinline handler: (T) -> Unit): PacketListener<T> {
        val listener = PacketListener(T::class, handler)
        listeners.add(listener)
        return listener
    }

    fun <T : AbstractPacket> listen(clazz: KClass<T>, handler: (T) -> Unit): PacketListener<T> {
        val listener = PacketListener(clazz, handler)
        listeners.add(listener)
        return listener
    }

    fun unregisterListener(listener: PacketListener<out AbstractPacket>) {
        listeners.remove(listener)
    }

    suspend fun publish(packet: AbstractPacket, vararg receivers: ServiceId): PacketResponse {
        packet.sender = serviceId
        val packedPacket = PackedPacket(gson.toJson(packet), packet::class.java.name)
        receivers.forEach {
            val targetTopic = databaseConnection.getClient().getTopic(it.toName())
            targetTopic.publish(packedPacket)
        }
        return PacketResponse(this, packet)
    }

    suspend fun publishAll(packet: AbstractPacket): PacketResponse {
        packet.sender = serviceId
        val packedPacket = PackedPacket(gson.toJson(packet), packet::class.java.name)
        return PacketResponse(this, packet)
    }

    suspend fun publish(packet: AbstractPacket, vararg serviceTypes: ServiceType): PacketResponse {
        packet.sender = serviceId
        val packedPacket = PackedPacket(gson.toJson(packet), packet::class.java.name)
        serviceTypes.forEach { typedTopics[it]?.publish(packedPacket) }
        return PacketResponse(this, packet)
    }

}