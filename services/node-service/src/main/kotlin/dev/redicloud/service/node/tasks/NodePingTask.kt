package dev.redicloud.service.node.tasks

import dev.redicloud.service.base.repository.pingService
import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.repository.suspendNode
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.delay

class NodePingTask(val nodeService: NodeService) : CloudTask() {

    override suspend fun execute(): Boolean {
        if (System.getProperty("redicloud.task.node-ping.disable") != null) return true
        val otherNodes = nodeService.nodeRepository.getConnectedNodes().filter { it.serviceId != nodeService.serviceId }
        val unreachableNodes = unreachableNodes(*otherNodes.map { it.serviceId }.toTypedArray())
        if (unreachableNodes.isEmpty()) return false

        delay(2000)

        val confirmedUnreachableNodes = unreachableNodes(*unreachableNodes.toTypedArray())
        if (confirmedUnreachableNodes.isEmpty()) return false

        confirmedUnreachableNodes.forEach { nodeService.nodeRepository.suspendNode(nodeService, it) }
        return false
    }

    private suspend fun unreachableNodes(vararg serviceIds: ServiceId): List<ServiceId> {
        return serviceIds.map { it to nodeService.nodeRepository.pingService(it) }
            .filter { it.second == -1L}.map { it.first }
    }

}