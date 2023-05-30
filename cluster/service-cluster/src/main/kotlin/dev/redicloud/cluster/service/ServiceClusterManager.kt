package dev.redicloud.cluster.service

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.ServiceId
import org.redisson.api.LocalCachedMapOptions
import org.redisson.api.RList
import org.redisson.api.RMap
import java.util.*

class ServiceClusterManager(
    val databaseConnection: DatabaseConnection,
    val serviceId: ServiceId,
    val tempId: UUID
) {

    private val connectedServices: RMap<String, ServiceClusterSession>
    private val registeredServices: RList<String>
    private val shutdownThread: Thread
    private var disconnected: Boolean = true

    init {
        if (!databaseConnection.isConnected()) throw Exception("Database connection is not connected")
        connectedServices = databaseConnection.client!!.getLocalCachedMap(
            "cluster:connected-services",
            LocalCachedMapOptions
                .defaults<String, ServiceClusterSession>()
                .storeMode(LocalCachedMapOptions.StoreMode.LOCALCACHE_REDIS)
                .syncStrategy(LocalCachedMapOptions.SyncStrategy.UPDATE)
        )
        registeredServices = databaseConnection.client!!.getList("cluster:registered-services")
        shutdownThread = Thread() {
            if (disconnected) return@Thread
            if (!databaseConnection.isConnected()) {
                throw Exception("Database connection is not connected! Cannot remove service from cluster")
            }
            connectedServices.remove(serviceId.toName())
        }
    }

    suspend fun connect() {
        if (!registeredServices.contains(serviceId.toName())) {
            registeredServices.add(serviceId.toName())
        }
        if (connectedServices.containsKey(serviceId.toName())) {
            //TODO: log
        }
        connectedServices[serviceId.toName()] = ServiceClusterSession(serviceId, System.currentTimeMillis(), tempId)
        disconnected = false
        Runtime.getRuntime().addShutdownHook(shutdownThread)
    }

    suspend fun disconnect() {
        disconnected = true
        if (!connectedServices.containsKey(serviceId.toName())) return
        connectedServices.remove(serviceId.toName())
    }

    suspend fun isConnected(): Boolean {
        val session = connectedServices[serviceId.toName()] ?: return false
        return session.sessionId == tempId
    }

    suspend fun isConnected(serviceId: ServiceId) = connectedServices.containsKey(serviceId.toName())

    suspend fun getConnectedServices(): List<ServiceClusterSession> {
        return connectedServices.values.toList()
    }

}