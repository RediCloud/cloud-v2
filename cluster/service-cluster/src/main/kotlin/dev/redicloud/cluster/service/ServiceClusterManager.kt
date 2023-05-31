package dev.redicloud.cluster.service

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.utils.ServiceId
import kotlinx.coroutines.runBlocking
import org.redisson.api.RList

open class ServiceClusterManager(
    databaseConnection: DatabaseConnection,
    val serviceId: ServiceId
) : DatabaseBucketRepository<CloudService>(databaseConnection, "cluster:service") {

    protected val connectedServices: RList<ServiceId>
    protected val registeredServices: RList<ServiceId>
    protected val shutdownThread: Thread

    init {
        connectedServices = databaseConnection.client!!.getList("cluster:service:connected")
        registeredServices = databaseConnection.client!!.getList("cluster:service:registered")

        shutdownThread = Thread() {
            runBlocking {
                if (!databaseConnection.isConnected()) {
                    throw Exception("Database connection is not connected! Cannot remove service from cluster")
                }
                connectedServices.remove(serviceId)
                val service = getService(serviceId)
                if(service == null) return@runBlocking
                if (service.isConnected()) {
                    service.currentSession()!!.endTime = System.currentTimeMillis()
                }
                if (service.unregisterAfterDisconnect()) {
                    registeredServices.remove(serviceId)
                    delete(serviceId.toName())
                }else {
                    set(serviceId.toName(), service)
                }
            }
        }
    }

    suspend fun getService(serviceId: ServiceId): CloudService? = get(serviceId.toName())

    suspend fun getRegisteredServices(): List<CloudService> =
        registeredServices.mapNotNull { getService(it) }

    suspend fun getConnectedServices(): List<CloudService> =
        connectedServices.mapNotNull { getService(it) }

}