package dev.redicloud.service.node.tasks.service

import dev.redicloud.api.server.factory.ICloudServerFactory
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.tasks.CloudTask

class CloudInvalidServerUnregisterTask(
    private val thisNodeId: ServiceId,
    private val serverRepository: ICloudServerRepository,
    private val serverFactory: ICloudServerFactory,
    private val nodeRepository: ICloudNodeRepository
) : CloudTask() {

    override suspend fun execute(): Boolean {
        val isMaster = nodeRepository.getMasterNode()?.serviceId == thisNodeId
        val registeredServers = serverRepository.getRegisteredServers()

        val unregisterQueue = serverFactory.getUnregisterQueue()
        registeredServers.forEach { server ->
            if (server.configurationTemplate.static || unregisterQueue.contains(server.serviceId)) {
                return@forEach
            }
            if (isMaster && server.state == CloudServerState.STOPPED) {
                serverFactory.queueUnregister(server.serviceId)
                return@forEach
            }
            if (isMaster && server.state == CloudServerState.STOPPING && server.currentOrLastSession() == null) {
                serverFactory.queueUnregister(server.serviceId)
                return@forEach
            }

        }

        return false
    }

}