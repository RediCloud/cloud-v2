package dev.redicloud.service.node.repository

import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.events.NodeConnectEvent
import dev.redicloud.service.node.events.NodeSuspendEvent
import dev.redicloud.utils.service.ServiceId

val LOGGER = LogManager.logger(NodeRepository::class)

suspend fun NodeRepository.connect(nodeService: NodeService) {
    val serviceId = nodeService.configuration.toServiceId()
    val node = if (existsNode(serviceId)) {
        getNode(serviceId)!!
    } else {
        createNode(CloudNode(serviceId, mutableListOf(), mutableListOf(), false))
    }
    node.startSession(nodeService.configuration.hostAddress)
    updateNode(node)
    nodeService.eventManager.fireEvent(NodeConnectEvent(node))
}

suspend fun NodeRepository.disconnect(nodeService: NodeService) {
    val serviceId = nodeService.configuration.toServiceId()
    val node = getNode(serviceId) ?: return
    node.endSession()
    nodeService.eventManager.fireEvent(NodeConnectEvent(node))
}

//TODO: unregister services etc...
suspend fun NodeRepository.suspendNode(nodeService: NodeService, serviceId: ServiceId) {
    val node = getNode(serviceId) ?: return
    val currentSession = node.currentSession() ?: return
    currentSession.suspended = true
    updateNode(node)
    nodeService.eventManager.fireEvent(NodeSuspendEvent(node))
}