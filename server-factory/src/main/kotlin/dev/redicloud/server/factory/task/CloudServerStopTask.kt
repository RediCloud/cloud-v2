package dev.redicloud.server.factory.task

import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.launch

class CloudServerStopTask(
    private val serviceId: ServiceId,
    private val serverRepository: ServerRepository,
    private val serverFactory: ServerFactory
) : CloudTask() {

    companion object {
        private val logger = LogManager.logger(CloudServerStopTask::class)
    }

    override suspend fun execute(): Boolean {
        var stopped = 0
        var total = 0
        serverFactory.stopQueue.forEach {
            val server = serverRepository.getServer<CloudServer>(it)
            if (server == null) {
                serverFactory.stopQueue.remove(it)
                return@forEach
            }
            if (server.hostNodeId == serviceId) {
                defaultScope.launch {
                    total++
                    try {
                        serverFactory.stopServer(it)
                        serverFactory.stopQueue.remove(it)
                    }catch (e: Exception) {
                        logger.severe("Failed to stop server ${server.name}", e)
                    }finally {
                        stopped++
                    }
                }
                return false
            }
        }
        while (stopped != total) {
            Thread.sleep(333)
        }
        return false
    }
}