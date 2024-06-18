package dev.redicloud.service.node.tasks.service

import dev.redicloud.api.server.factory.ICloudServerFactory
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.tasks.CloudTask

class CloudInvalidServerUnregisterTask(
    private val serverRepository: ICloudServerRepository,
    private val serverFactory: ICloudServerFactory
) : CloudTask() {

    override suspend fun execute(): Boolean {
        val registeredServers = serverRepository.getRegisteredServers()

        val unregisterQueue = serverFactory.getUnregisterQueue()
        registeredServers.forEach { server ->
            if (server.configurationTemplate.static) {
                return@forEach
            }
            if (server.state == CloudServerState.STOPPED && !unregisterQueue.contains(server.serviceId)) {
                serverFactory.queueUnregister(server.serviceId)
            }
        }

        return false
    }

}