package dev.redicloud.service.base

import dev.redicloud.cluster.service.ServiceClusterManager
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.config.DatabaseConfig
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.ServiceId
import kotlinx.coroutines.runBlocking
import java.util.*

abstract class BaseService(
    private val databaseConfig: DatabaseConfig,
    val serviceId: ServiceId
) {

    companion object {
        lateinit var INSTANCE: BaseService
    }

    val tempId: UUID = UUID.randomUUID()
    val databaseConnection: DatabaseConnection
    val serviceClusterManager: ServiceClusterManager
    val packetManager: PacketManager

    init {
        databaseConnection = DatabaseConnection(databaseConfig, serviceId, GsonCodec())
        try {
            databaseConnection.connect()
        }catch (e: Exception) {
            throw Exception("Failed to connect to database", e)
        }

        serviceClusterManager = ServiceClusterManager(databaseConnection, serviceId, tempId)
        runBlocking { serviceClusterManager.connect() }

        packetManager = PacketManager(databaseConnection, serviceId)



        INSTANCE = this
    }

}