package dev.redicloud.repository.service

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.runBlocking
import org.redisson.api.RList

abstract class ServiceRepository<T : CloudService>(
    databaseConnection: DatabaseConnection,
    val serviceId: ServiceId,
    serviceType: ServiceType,
    val packetManager: PacketManager
) : DatabaseBucketRepository<T>(databaseConnection, "service:${serviceType.name.lowercase()}") {

    protected val connectedServices: RList<ServiceId>
    protected val registeredServices: RList<ServiceId>
    protected val shutdownThread: Thread

    init {
        connectedServices = databaseConnection.getClient().getList("service:connected")
        registeredServices = databaseConnection.getClient().getList("service:registered")

        shutdownThread = Thread() {
            runBlocking {
                if (!databaseConnection.isConnected()) {
                    throw Exception("Database connection is not connected! Cannot remove service from cluster")
                }
                connectedServices.remove(serviceId)
                val service = getService(serviceId) as T? ?: return@runBlocking
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

    suspend fun createService(cloudService: CloudService): CloudService {
        getUnsafeHandle<CloudService>(cloudService.serviceId.toDatabaseIdentifier(), true).set(cloudService)
        if (cloudService.isConnected() && !connectedServices.contains(cloudService.serviceId)) {
            connectedServices.add(cloudService.serviceId)
        }
        registeredServices.add(cloudService.serviceId)
        return cloudService
    }

    suspend fun updateService(cloudService: CloudService): CloudService {
        getUnsafeHandle<CloudService>(cloudService.serviceId.toDatabaseIdentifier(), true).set(cloudService)
        if (cloudService.isConnected() && !connectedServices.contains(cloudService.serviceId)) {
            connectedServices.add(cloudService.serviceId)
        }else if(!cloudService.isConnected()) {
            connectedServices.remove(cloudService.serviceId)
        }
        if (!registeredServices.contains(cloudService.serviceId)) {
            registeredServices.add(cloudService.serviceId)
        }
        return cloudService
    }

    suspend fun getRegisteredServices(): List<CloudService> =
        registeredServices.mapNotNull { getService(it) }

    suspend fun getConnectedServices(): List<CloudService> =
        connectedServices.mapNotNull { getService(it) }

}