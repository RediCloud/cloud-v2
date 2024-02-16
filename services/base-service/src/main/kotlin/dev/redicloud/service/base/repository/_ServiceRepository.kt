package dev.redicloud.service.base.repository

import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.service.CachedServiceRepository
import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.service.base.packets.ServicePingPacket
import dev.redicloud.service.base.packets.ServicePingResponse
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import kotlin.time.Duration.Companion.seconds

suspend fun ServerRepository.pingService(serviceId: ServiceId): Long {
    return when (serviceId.type) {
        ServiceType.MINECRAFT_SERVER -> internalMinecraftServerRepository.pingService(serviceId)
        ServiceType.PROXY_SERVER -> internalProxyServerRepository.pingService(serviceId)
        else -> throw IllegalArgumentException("Unknown service type ${serviceId.type} (${serviceId.type})")
    }
}

suspend fun NodeRepository.pingService(serviceId: ServiceId): Long {
    return internalRepo.pingService(serviceId)
}

suspend fun ServiceRepository.pingService(serviceId: ServiceId): Long {
    if (serviceId == databaseConnection.serviceId) return 0L
    val start = System.currentTimeMillis()
    val response = packetManager.publish(ServicePingPacket(), serviceId)
        .withTimeOut(5.seconds)
        .waitBlocking()
        ?: return -1L
    if (response !is ServicePingResponse) return -1L
    return response.receivedPingTime - start
}

suspend fun CachedServiceRepository<*, *>.pingService(serviceId: ServiceId): Long {
    if (serviceId == databaseConnection.serviceId) return 0L
    val start = System.currentTimeMillis()
    val response = packetManager.publish(ServicePingPacket(), serviceId)
        .withTimeOut(5.seconds)
        .waitBlocking()
        ?: return -1L
    if (response !is ServicePingResponse) return -1L
    return response.receivedPingTime - start
}