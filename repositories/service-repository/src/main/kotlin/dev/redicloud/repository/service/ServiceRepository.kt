package dev.redicloud.repository.service

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.utils.ServiceId
import dev.redicloud.utils.ServiceType
import kotlinx.coroutines.runBlocking
import org.redisson.api.RList

abstract class ServiceRepository<T : CloudService>(
    databaseConnection: DatabaseConnection,
    val serviceId: ServiceId,
    serviceType: ServiceType
) : DatabaseBucketRepository<T>(databaseConnection, "service:${serviceType.name.lowercase()}") {

    protected val connectedServices: RList<ServiceId>
    protected val registeredServices: RList<ServiceId>
    protected val shutdownThread: Thread

    init {
        connectedServices = databaseConnection.client!!.getList("service:connected")
        registeredServices = databaseConnection.client!!.getList("service:registered")

        shutdownThread = Thread() {
            runBlocking {
                if (!databaseConnection.isConnected()) {
                    throw Exception("Database connection is not connected! Cannot remove service from cluster")
                }
                connectedServices.remove(serviceId)
                val service = getService(serviceId) as T?
                if(service == null) return@runBlocking
                if (service.isConnected()) {
                    service.currentSession()!!.endTime = System.currentTimeMillis()
                }
                if (service.unregisterAfterDisconnect()) {
                    registeredServices.remove(serviceId)
                    delete(serviceId.id.toString())
                }else {
                    set(serviceId.id.toString(), service)
                }
            }
        }
    }

    suspend fun getService(serviceId: ServiceId): CloudService?
        = getUnsafeHandle<CloudService>(serviceId.toDatabaseIdentifier(), true).get()

    suspend fun existsService(serviceId: ServiceId): Boolean
        = getUnsafeHandle<CloudService>(serviceId.toDatabaseIdentifier(), true).isExists

    suspend fun getRegisteredServices(): List<CloudService> =
        registeredServices.mapNotNull { getService(it) }

    suspend fun getConnectedServices(): List<CloudService> =
        connectedServices.mapNotNull { getService(it) }

}