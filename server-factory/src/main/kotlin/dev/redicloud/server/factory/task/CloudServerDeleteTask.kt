package dev.redicloud.server.factory.task

import dev.redicloud.logging.LogManager
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.launch

class CloudServerDeleteTask(
    private val serverFactory: ServerFactory
) : CloudTask() {

    companion object {
        private val LOGGER = LogManager.logger(CloudServerDeleteTask::class)
    }

    override suspend fun execute(): Boolean {
        var total = 0
        var responded = 0
        serverFactory.deleteQueue.forEach { queued ->
            total++
            serverFactory.deleteQueue.remove(queued)
            defaultScope.launch {
                try {
                    serverFactory.deleteServer(queued)
                }catch (e: Exception) {
                    LOGGER.severe("§cFailed to delete server ${queued.toName()}!")
                }finally {
                    responded++
                }
            }
        }

        while (responded < total) {
            Thread.sleep(100)
        }

        return false
    }

}