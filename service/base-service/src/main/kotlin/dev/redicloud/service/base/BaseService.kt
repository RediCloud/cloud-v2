package dev.redicloud.service.base

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.config.DatabaseConfig
import dev.redicloud.packets.PacketManager
import dev.redicloud.service.base.cluster.BaseServiceClusterManager
import dev.redicloud.utils.ServiceId
import kotlinx.coroutines.runBlocking

abstract class BaseService(
    databaseConfig: DatabaseConfig,
    serviceId: ServiceId
) {

    val databaseConnection: DatabaseConnection
    val serviceClusterManager: BaseServiceClusterManager
    val packetManager: PacketManager

    init {
        databaseConnection = DatabaseConnection(databaseConfig, serviceId, GsonCodec())
        try {
            databaseConnection.connect()
        } catch (e: Exception) {
            throw Exception("Failed to connect to database", e)
        }

        serviceClusterManager = BaseServiceClusterManager(databaseConnection, serviceId)
        runBlocking { serviceClusterManager.connect() }

        packetManager = PacketManager(databaseConnection, serviceId)
    }

}