package dev.redicloud.service.node.tasks

import dev.redicloud.logging.LogManager
import dev.redicloud.service.node.NodeService
import dev.redicloud.tasks.CloudTask
import java.util.logging.Level

class NodeSelfSuspendTask(val nodeService: NodeService) : CloudTask() {

    companion object {
        val LOGGER = LogManager.logger(NodeSelfSuspendTask::class)
    }

    override suspend fun execute(): Boolean {
        val node = nodeService.nodeRepository.getNode(nodeService.serviceId) ?: return false
        if (!node.isSuspended()) return false
        LOGGER.log(Level.WARNING, "Node is suspended, suspending service...")
        nodeService.shutdown()
        return false
    }

}