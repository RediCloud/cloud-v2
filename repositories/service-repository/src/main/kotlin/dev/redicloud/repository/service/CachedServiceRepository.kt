package dev.redicloud.repository.service

import dev.redicloud.api.repositories.service.ICloudService
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.runBlocking
import org.redisson.api.RList
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.time.Duration

abstract class CachedServiceRepository<I : ICloudService, K : CloudService>(
    val databaseConnection: DatabaseConnection,
    val targetServiceType: ServiceType,
    val packetManager: PacketManager,
    interfaceClass: KClass<I>,
    implClass: KClass<K>,
    cacheDuration: Duration,
    private val targetRepository: ServiceRepository,
    vararg cacheTypes: ServiceType
) : CachedDatabaseBucketRepository<I, K>(
    databaseConnection,
    "service:${targetServiceType.name.lowercase()}",
    null,
    interfaceClass,
    implClass,
    cacheDuration,
    packetManager,
    *cacheTypes
) {

    internal val shutdownAction: Runnable
    internal var shutdownCalled = false

    init {
        shutdownAction = Runnable {
            if (shutdownCalled) return@Runnable
            shutdownCalled = true
            runBlocking {
                if (!databaseConnection.isConnected()) {
                    throw Exception("Database connection is not connected! Cannot remove service from cluster")
                }
                val serviceId = databaseConnection.serviceId
                if (serviceId.type != targetServiceType) return@runBlocking
                connectedServices.remove(serviceId)
                val service = getService(serviceId) ?: return@runBlocking
                service.connected = false
                if (service.currentSession != null) service.endSession()
                if (service.canSelfUnregister() && service.unregisterAfterDisconnect()) {
                    registeredServices.remove(serviceId)
                    deleteService(interfaceClass.cast(transformShutdownable(service)))
                }else {
                    updateService(interfaceClass.cast(transformShutdownable(service)))
                }
            }
        }
    }

    val connectedServices: RList<ServiceId>
        get() {
            return targetRepository.connectedServices
        }

    val registeredServices: RList<ServiceId>
        get() {
            return targetRepository.registeredServices
        }

    suspend fun getService(serviceId: ServiceId): K? {
        if (serviceId.type != targetServiceType) throw IllegalArgumentException("Service type does not match (expected ${targetServiceType.name}, got ${serviceId.type.name})")
        return get(serviceId.id.toString())
    }

    suspend fun getService(name: String): K? {
        return getRegisteredServices().firstOrNull { it.name.lowercase() == name.lowercase() }
    }

    suspend fun existsService(serviceId: ServiceId): Boolean {
        if (serviceId.type != targetServiceType) throw IllegalArgumentException("Service type does not match (expected ${targetServiceType.name}, got ${serviceId.type.name})")
        return exists(serviceId.id.toString())
    }

    suspend fun createService(cloudService: I): K {
        if (cloudService.serviceId.type != targetServiceType) throw IllegalArgumentException("Service type does not match (expected ${targetServiceType.name}, got ${cloudService.serviceId.type.name})")
        return set(cloudService.serviceId.id.toString(), cloudService).also {
            if (it.connected && !connectedServices.contains(it.serviceId)) {
                connectedServices.add(it.serviceId)
            }else if(!it.connected) {
                connectedServices.remove(it.serviceId)
            }
            if (!registeredServices.contains(it.serviceId)) {
                registeredServices.add(it.serviceId)
            }
        }
    }

    suspend fun updateService(cloudService: I): K {
        if (cloudService.serviceId.type != targetServiceType) throw IllegalArgumentException("Service type does not match (expected ${targetServiceType.name}, got ${cloudService.serviceId.type.name})")
        return set(cloudService.serviceId.id.toString(), cloudService).also {
            if (it.connected && !connectedServices.contains(it.serviceId)) {
                connectedServices.add(it.serviceId)
            }else if(!it.connected) {
                connectedServices.remove(it.serviceId)
            }
            if (!registeredServices.contains(it.serviceId)) {
                registeredServices.add(it.serviceId)
            }
        }
    }

    suspend fun deleteService(cloudService: I): Boolean {
        val state = delete(cloudService.serviceId.id.toString())
        connectedServices.remove(cloudService.serviceId)
        registeredServices.remove(cloudService.serviceId)
        return state
    }

    suspend fun getRegisteredServices(): List<K> {
        return registeredServices.mapNotNull { getService(it) }
    }

    suspend fun getConnectedServices(): List<K> {
        return connectedServices.mapNotNull { getService(it) }
    }

    suspend fun getRegisteredServiceIds(): List<ServiceId> {
        return registeredServices.toList()
    }

    suspend fun getConnectedServiceIds(): List<ServiceId> {
        return connectedServices.toList()
    }

    abstract suspend fun transformShutdownable(service: K): K

}