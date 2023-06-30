package dev.redicloud.service.node.repository.node

import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.service.base.events.NodeConnectEvent
import dev.redicloud.service.base.events.NodeDisconnectEvent
import dev.redicloud.service.base.events.NodeSuspendedEvent
import dev.redicloud.service.node.NodeService
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.toMb

val LOGGER = LogManager.logger(NodeRepository::class)

suspend fun NodeRepository.connect(nodeService: NodeService) {
    val serviceId = nodeService.configuration.toServiceId()
    val node = if (existsNode(serviceId)) {
        getNode(serviceId)!!
    } else {
        val total = Runtime.getRuntime().totalMemory()
        val free = Runtime.getRuntime().freeMemory()

        val allocated = total - free
        val actualFree: Long = Runtime.getRuntime().maxMemory() - allocated
        val memory = toMb((actualFree * 0.9).toLong())
        if (memory < 1024) throw IllegalStateException("There must be at least 1GB of free memory to start a node!")
        createNode(CloudNode(serviceId, nodeService.configuration.nodeName, mutableListOf(), mutableListOf(), false, memory))
    }
    node.startSession(nodeService.configuration.hostAddress)
    updateNode(node)
    nodeService.eventManager.fireEvent(NodeConnectEvent(node.serviceId))
    LOGGER.info("Connected to node cluster!")
}

suspend fun NodeRepository.disconnect(nodeService: NodeService) {
    val serviceId = nodeService.configuration.toServiceId()
    val node = getNode(serviceId) ?: return
    node.endSession()
    if (node.master) node.master = false
    updateNode(node)
    nodeService.eventManager.fireEvent(NodeDisconnectEvent(node.serviceId))
    LOGGER.info("Disconnected from node cluster!")
}

//TODO: unregister services etc...
suspend fun NodeRepository.suspendNode(nodeService: NodeService, serviceId: ServiceId) {
    val node = getNode(serviceId) ?: return
    val currentSession = node.currentSession() ?: return
    currentSession.suspended = true
    updateNode(node)
    nodeService.eventManager.fireEvent(NodeSuspendedEvent(node.serviceId, getNode(this.serviceId)!!))
}
