package dev.redicloud.service.node.repository.node

import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.service.ServiceSessions
import dev.redicloud.api.events.internal.node.NodeConnectEvent
import dev.redicloud.api.events.internal.node.NodeSuspendedEvent
import dev.redicloud.service.node.NodeService
import dev.redicloud.api.service.ServiceId
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
        //TODO: Update current memory by task
        createNode(CloudNode(serviceId, nodeService.configuration.nodeName, ServiceSessions(), mutableListOf(), false, toMb(allocated), memory))
    }
    node.currentMemoryUsage = 0
    node.hostedServers.clear()
    node.startSession(nodeService.configuration.hostAddress)
    node.connected = true
    updateNode(node)
    nodeService.eventManager.fireEvent(NodeConnectEvent(node.serviceId))
}

//TODO: unregister services etc...
suspend fun NodeRepository.suspendNode(nodeService: NodeService, serviceId: ServiceId) {
    val node = getNode(serviceId) ?: return
    val currentSession = node.currentSession ?: return
    currentSession.suspended = true
    node.connected = false
    updateNode(node)
    nodeService.eventManager.fireEvent(NodeSuspendedEvent(node.serviceId, nodeService.serviceId))
}
