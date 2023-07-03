package dev.redicloud.service.node.tasks.node

import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.service.base.events.node.NodeMasterChangedEvent
import dev.redicloud.tasks.CloudTask

class NodeChooseMasterTask(private val nodeRepository: NodeRepository) : CloudTask() {

    override suspend fun execute(): Boolean {
        val nodes = nodeRepository.getConnectedNodes()
        if (nodes.any { it.master }) return false

        val sessions = nodes.mapNotNull { it.currentSession() }
        val oldest = sessions.minByOrNull { it.startTime }?.serviceId ?: return false
        val node = nodes.first { it.currentSession()?.serviceId == oldest }
        if (node.serviceId != nodeRepository.serviceId) return false
        node.master = true
        nodeRepository.updateNode(node)
        getEventManager().fireEvent(NodeMasterChangedEvent(node.serviceId, null))
        return false
    }

}