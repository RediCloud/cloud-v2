package dev.redicloud.repository.server

import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.service.CachedServiceRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.minutes

class InternalServerRepository<I : ICloudServer, K : CloudServer>(
    databaseConnection: DatabaseConnection,
    packetManager: PacketManager,
    interfaceClass: KClass<I>,
    implementationClass: KClass<K>,
    targetType: ServiceType,
    targetRepository: ServerRepository
) : CachedServiceRepository<I, K>(
    databaseConnection,
    targetType,
    packetManager,
    interfaceClass,
    implementationClass,
    5.minutes,
    targetRepository
) {
    override suspend fun transformShutdownable(service: K): K {
        service.state = CloudServerState.STOPPED
        service.connected = false
        return service
    }
}