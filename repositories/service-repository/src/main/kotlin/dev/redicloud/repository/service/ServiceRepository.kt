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
    val shutdownAction: Runnable
    private var shutdownCalled = false

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
                if (serviceId.type != serviceType) return@runBlocking
                connectedServices.remove(serviceId)
                val service = getService(serviceId) as T? ?: return@runBlocking
                service.connected = false
                if (service.currentSession() != null) service.endSession()
                if (!service.unregisterAfterDisconnect()) return@runBlocking
                if (service.canSelfUnregister()) {
                    registeredServices.remove(serviceId)
                    deleteService(transformShutdownable(service))
                }else {
                    updateService(transformShutdownable(service))
                }
            }
        }
    }

    abstract suspend fun transformShutdownable(service: T): T

    protected suspend fun <C : CloudService> getService(serviceId: ServiceId): C?
        = getUnsafeHandle<C>(serviceId.toDatabaseIdentifier(), true).get()

    protected suspend fun <C : CloudService> existsService(serviceId: ServiceId): Boolean
        = getUnsafeHandle<C>(serviceId.toDatabaseIdentifier(), true).isExists

    protected suspend fun <C : CloudService> createService(cloudService: C): C {
        getUnsafeHandle<C>(cloudService.serviceId.toDatabaseIdentifier(), true).set(cloudService)
        if (cloudService.isConnected() && !connectedServices.contains(cloudService.serviceId)) {
            connectedServices.add(cloudService.serviceId)
        }
        if (!registeredServices.contains(cloudService.serviceId)) {
            registeredServices.add(cloudService.serviceId)
        }
        return cloudService
    }

    protected suspend fun <C : CloudService> updateService(cloudService: C): CloudService {
        getUnsafeHandle<C>(cloudService.serviceId.toDatabaseIdentifier(), true).set(cloudService)
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

    protected suspend fun <C : CloudService> deleteService(cloudService: C) {
        getUnsafeHandle<C>(cloudService.serviceId.toDatabaseIdentifier(), true).delete()
        connectedServices.remove(cloudService.serviceId)
        registeredServices.remove(cloudService.serviceId)
    }

    protected suspend fun getRegisteredServices(): List<CloudService> =
        registeredServices.mapNotNull { getService(it) }

    protected suspend fun getConnectedServices(): List<CloudService> =
        connectedServices.mapNotNull { getService(it) }

    protected suspend fun getRegisteredIds(): List<ServiceId> =
        registeredServices.toList()

    protected suspend fun getConnectedIds(): List<ServiceId> =
        connectedServices.toList()

}