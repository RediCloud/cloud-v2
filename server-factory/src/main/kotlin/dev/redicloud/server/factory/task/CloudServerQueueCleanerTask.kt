package dev.redicloud.server.factory.task

import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask
import kotlin.time.Duration.Companion.minutes

class CloudServerQueueCleanerTask(
    private val serverFactory: ServerFactory,
    private val nodeRepository: NodeRepository
) : CloudTask() {

    companion object {
        private val logger = LogManager.logger(CloudServerQueueCleanerTask::class)
        val MAX_QUEUE_TIME = System.getProperty("redicloud.server.factory.max-queue-time", 3.minutes.inWholeMilliseconds.toString()).toLong()

    }

    override suspend fun execute(): Boolean {
        val nodes = nodeRepository.getConnectedNodes()
        val masterNode = nodes.firstOrNull { it.master }
        if (masterNode?.serviceId != nodeRepository.serviceId) return false

        serverFactory.getStartList().forEach { info ->
            // Check if queue time is too long
            if ((System.currentTimeMillis() - info.queueTime) - MAX_QUEUE_TIME > 0) {
                logger.warning("§cStart of template ${info.configurationTemplate.name} took too long, cancelling server start!")
                serverFactory.startQueue.removeIf { it.uniqueId == info.uniqueId }
                return@forEach
            }
            // Check if no node is available
            if (info.nodeStartOrder.isEmpty()) {
                logger.warning("§cNo node for template ${info.configurationTemplate.name} available, cancelling server start!")
            }
            // Check if all nodes failed to start
            if (info.failedStarts.map { it.key to it.value.values.sum() }.count { it.second != 0 } > nodes.size) {
                logger.warning("§cAll nodes for template ${info.configurationTemplate.name} failed to start, cancelling server start!")
                serverFactory.startQueue.removeIf { it.uniqueId == info.uniqueId }
                return@forEach
            }
        }

        return false
    }

}