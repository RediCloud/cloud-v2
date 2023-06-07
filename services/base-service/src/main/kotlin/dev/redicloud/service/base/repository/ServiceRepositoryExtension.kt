package dev.redicloud.service.base.repository

import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.service.base.packets.ServicePingPacket
import dev.redicloud.service.base.packets.ServicePingResponse
import dev.redicloud.utils.service.ServiceId
import kotlin.time.Duration.Companion.seconds

suspend fun ServiceRepository<*>.pingService(serviceId: ServiceId): Long {
    if (serviceId == this.serviceId) return 0L
    val start = System.currentTimeMillis()
    val response = packetManager.publish(ServicePingPacket(), serviceId)
        .withTimeOut(5.seconds)
        .waitBlocking() ?: return -1L
    if (response !is ServicePingResponse) return -1L
    return start - System.currentTimeMillis()
}