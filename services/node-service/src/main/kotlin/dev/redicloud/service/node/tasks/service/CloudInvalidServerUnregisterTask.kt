package dev.redicloud.service.node.tasks.service

import dev.redicloud.api.server.factory.ICloudServerFactory
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.service.server.ICloudServer
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
            val isStatic = server.configurationTemplate.static
            val hasSession = server.currentOrLastSession() != null
            val state = server.state
            val hostId = server.hostNodeId
            if (unregisterQueue.contains(server.serviceId)) {
                return@forEach
            }
            if (isMaster && state == CloudServerState.STOPPED && !isStatic) {
                stopOrUnregister(server)
                return@forEach
            }
            if (isMaster && state == CloudServerState.STOPPING && !hasSession) {
                stopOrUnregister(server)
                return@forEach
            }
            if (thisNodeId == hostId && state != CloudServerState.STOPPED && serverFactory.hostedProcesses.none { it.serverId == server.serviceId }) {
                stopOrUnregister(server)
                return@forEach
            }
            val nodes = nodeRepository.getConnectedNodes()
            if (isMaster && nodes.none { it.serviceId == hostId } && state != CloudServerState.STOPPED) {
                stopOrUnregister(server)
                return@forEach
            }
        }

        return false
    }

    private suspend fun stopOrUnregister(server: ICloudServer) {
        if (server.configurationTemplate.static) {
            serverFactory.queueStop(server.serviceId, true)
        }else {
            serverFactory.queueUnregister(server.serviceId)
        }
    }

}