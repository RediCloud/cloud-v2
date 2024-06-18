package dev.redicloud.server.factory.task

import dev.redicloud.logging.LogManager
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.MultiAsyncAction

class CloudServerDeleteTask(
    private val serverFactory: ServerFactory
) : CloudTask() {

    companion object {
        private val LOGGER = LogManager.logger(CloudServerDeleteTask::class)
    }

    override suspend fun execute(): Boolean {
        val actions = MultiAsyncAction()
        serverFactory.deleteQueue.forEach { queued ->
            serverFactory.deleteQueue.remove(queued)
            actions.add {
                try {
                    serverFactory.deleteServer(queued)
                }catch (e: Exception) {
                    LOGGER.severe("§cFailed to delete server ${queued.toName()}!", e)
                }
            }
        }

        actions.joinAll()

        return false
    }

}