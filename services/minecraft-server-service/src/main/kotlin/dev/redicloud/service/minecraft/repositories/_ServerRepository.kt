package dev.redicloud.service.minecraft.repositories

import dev.redicloud.api.events.impl.server.CloudServerConnectedEvent
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.api.service.ServiceId

suspend fun ServerRepository.connect(serviceId: ServiceId): ServiceId {
    val thisServer = this.getServer<CloudServer>(serviceId) ?: throw IllegalStateException("Server not found")
    thisServer.connected = true
    thisServer.startSession(System.getenv("RC_HOST"))
    updateServer(thisServer)
    eventManager.fireEvent(CloudServerConnectedEvent(serviceId))
    return thisServer.hostNodeId
}