package dev.redicloud.server.factory.task

import dev.redicloud.logging.LogManager
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.MultiAsyncAction

class CloudServerTransferTask(
    private val serverFactory: ServerFactory
) : CloudTask() {

    companion object {
        private val logger = LogManager.logger(CloudServerTransferTask::class)
    }

    override suspend fun execute(): Boolean {
        val actions = MultiAsyncAction()
        serverFactory.transferQueue.forEach {
            actions.add {
                try {
                    serverFactory.transferQueue.remove(it)
                    serverFactory.transferServer(it.serverId, it.targetNodeId)
                }catch (e: Exception) {
                    logger.severe("§cError while transferring server ${it.serverId.toName()} to node ${it.targetNodeId.toName()}", e)
                }
            }
        }
        actions.joinAll()

        return false
    }

}