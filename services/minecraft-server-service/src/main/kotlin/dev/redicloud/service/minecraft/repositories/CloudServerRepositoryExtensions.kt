package dev.redicloud.service.minecraft.repositories

import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.utils.service.ServiceId

suspend fun ServerRepository.connect(serviceId: ServiceId) {
    val thisServer = this.getServer<CloudServer>(serviceId) ?: throw IllegalStateException("Server not found")
    thisServer.connected = true
    updateServer(thisServer)
}

suspend fun ServerRepository.disconnect(serviceId: ServiceId) {
    val thisServer = this.getServer<CloudServer>(serviceId) ?: throw IllegalStateException("Server not found")
    thisServer.connected = false
    updateServer(thisServer)
}