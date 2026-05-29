package dev.redicloud.service.node.tasks.node

import dev.redicloud.service.base.repository.pingService
import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.repository.node.suspendNode
import dev.redicloud.tasks.CloudTask
import dev.redicloud.api.service.ServiceId
import kotlinx.coroutines.delay

class NodePingTask(val nodeService: NodeService) : CloudTask() {

    companion object {
        private const val UNREACHABLE_CONFIRM_DELAY_MS = 2000L
    }

    override suspend fun execute(): Boolean {
        if (System.getProperty("redicloud.task.node-ping.disable") != null) return true
        val otherNodes = nodeService.nodeRepository.getConnectedNodes().filter { it.serviceId != nodeService.serviceId }
        val unreachableNodes = unreachableNodes(*otherNodes.map { it.serviceId }.toTypedArray())
        if (unreachableNodes.isEmpty()) return false

        delay(UNREACHABLE_CONFIRM_DELAY_MS)

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