package dev.redicloud.repository.service

import dev.redicloud.api.database.grid.list.ISyncedMutableList
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.api.service.ServiceId
import kotlinx.coroutines.runBlocking

abstract class ServiceRepository(
    val databaseConnection: DatabaseConnection,
    val packetManager: PacketManager
) {

    val connectedServices: ISyncedMutableList<ServiceId>
    val registeredServices: ISyncedMutableList<ServiceId>
    val shutdownAction: Runnable
    private var shutdownCalled = false
    val internalRepositories: MutableList<CachedServiceRepository<*, *>> = mutableListOf()

    init {
        connectedServices = databaseConnection.getMutableList("cloud:service:connected")
        registeredServices = databaseConnection.getMutableList("cloud:service:registered")

        shutdownAction = Runnable {
            if (shutdownCalled) return@Runnable
            shutdownCalled = true
            runBlocking {
                if (!databaseConnection.connected) {
                    throw Exception("Database connection is not connected! Cannot remove service from cluster")
                }
                internalRepositories.forEach { it.shutdownAction.run() }
            }
        }
    }

}