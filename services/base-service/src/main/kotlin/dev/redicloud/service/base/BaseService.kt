package dev.redicloud.service.base

import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.service.base.packets.ServicePingPacket
import dev.redicloud.service.base.packets.ServicePingResponse
import dev.redicloud.tasks.CloudTaskManager
import dev.redicloud.utils.service.ServiceId

abstract class BaseService(
    databaseConfiguration: DatabaseConfiguration,
    val serviceId: ServiceId
) {

    companion object {
        val LOGGER = LogManager.logger(BaseService::class)
        var SHUTTINGDOWN = false
    }

    val databaseConnection: DatabaseConnection

    val nodeRepository: NodeRepository
    val serverRepository: ServerRepository

    val packetManager: PacketManager
    val eventManager: EventManager
    val taskManager: CloudTaskManager

    init {
        databaseConnection = DatabaseConnection(databaseConfiguration, serviceId, GsonCodec())
        try {
            databaseConnection.connect()
        } catch (e: Exception) {
            throw Exception("Failed to connect to database", e)
        }
        packetManager = PacketManager(databaseConnection, serviceId)
        eventManager = EventManager(packetManager)
        taskManager = CloudTaskManager(eventManager, packetManager)

        nodeRepository = NodeRepository(databaseConnection, serviceId, packetManager)
        serverRepository = ServerRepository(databaseConnection, serviceId, packetManager)

        this.registerPackets()
    }

    open fun shutdown() {
        SHUTTINGDOWN = true
        taskManager.getTasks().forEach { it.cancel() }
        databaseConnection.disconnect()
    }

    private fun registerPackets() {
        packetManager.registerPacket(ServicePingPacket())
        packetManager.registerPacket(ServicePingResponse())
    }

}