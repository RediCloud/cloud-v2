package dev.redicloud.service.base

import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.event.EventManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.utils.ServiceId

abstract class BaseService(
    databaseConfiguration: DatabaseConfiguration,
    serviceId: ServiceId
) {

    val databaseConnection: DatabaseConnection

    val nodeRepository: NodeRepository
    val serverRepository: ServerRepository

    val packetManager: PacketManager
    val eventManager: EventManager

    init {
        databaseConnection = DatabaseConnection(databaseConfiguration, serviceId, GsonCodec())
        try {
            databaseConnection.connect()
        } catch (e: Exception) {
            throw Exception("Failed to connect to database", e)
        }

        nodeRepository = NodeRepository(databaseConnection, serviceId)
        serverRepository = ServerRepository(databaseConnection, serviceId)

        packetManager = PacketManager(databaseConnection, serviceId)
        eventManager = EventManager(packetManager)
    }

}