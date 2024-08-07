package dev.redicloud.service.node.tasks.node

import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.api.events.internal.node.NodeMasterChangedEvent
import dev.redicloud.event.EventManager
import dev.redicloud.tasks.CloudTask
import dev.redicloud.api.service.ServiceId

class NodeChooseMasterTask(
    private val serviceId: ServiceId,
    private val nodeRepository: NodeRepository,
    private val eventManager: EventManager
) : CloudTask() {

    override suspend fun execute(): Boolean {
        val nodes = nodeRepository.getConnectedNodes()
        if (nodes.any { it.master }) return false

        val sessions = nodes.mapNotNull { it.currentSession }
        val oldest = sessions.minByOrNull { it.startTime }?.serviceId ?: return false
        val node = nodes.first { it.currentSession?.serviceId == oldest }
        if (node.serviceId != serviceId) return false
        node.master = true
        nodeRepository.updateNode(node)
        eventManager.fireEvent(NodeMasterChangedEvent(node.serviceId, null))
        return false
    }

}