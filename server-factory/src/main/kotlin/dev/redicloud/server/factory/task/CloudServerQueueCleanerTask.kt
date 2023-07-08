package dev.redicloud.server.factory.task

import dev.redicloud.console.commands.toConsoleValue
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
            val name = if (info.configurationTemplate != null) {
                info.configurationTemplate.name
            } else if (info.serviceId != null) {
                info.serviceId.toName()
            } else null

            if (name == null) {
                logger.warning("§cCould not get name of server to start, cancelling server start!")
                serverFactory.startQueue.remove(info)
                return@forEach
            }

            // Check if queue time is too long
            if ((System.currentTimeMillis() - info.queueTime) - MAX_QUEUE_TIME > 0) {
                logger.warning("§cStart of template ${toConsoleValue(name, false)} took too long, cancelling server start!")
                serverFactory.startQueue.remove(info)
                return@forEach
            }
            // Check if no node is available
            if (info.nodeStartOrder.isEmpty()) {
                logger.warning("§cNo node for template ${toConsoleValue(name, false)} available, cancelling server start!")
                serverFactory.startQueue.remove(info)
                return@forEach
            }
        }
        return false
    }

}