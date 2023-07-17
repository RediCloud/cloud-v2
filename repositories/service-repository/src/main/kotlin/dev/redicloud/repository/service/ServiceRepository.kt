package dev.redicloud.repository.service

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.runBlocking
import org.redisson.api.RList

abstract class ServiceRepository(
    val databaseConnection: DatabaseConnection,
    val packetManager: PacketManager
) {

    val connectedServices: RList<ServiceId>
    val registeredServices: RList<ServiceId>
    val shutdownAction: Runnable
    private var shutdownCalled = false
    val internalRepositories: MutableList<CachedServiceRepository<*, *>> = mutableListOf()

    init {
        connectedServices = databaseConnection.getClient().getList("service:connected")
        registeredServices = databaseConnection.getClient().getList("service:registered")

        shutdownAction = Runnable {
            if (shutdownCalled) return@Runnable
            shutdownCalled = true
            runBlocking {
                if (!databaseConnection.isConnected()) {
                    throw Exception("Database connection is not connected! Cannot remove service from cluster")
                }
                internalRepositories.forEach { it.shutdownAction.run() }
            }
        }
    }

}