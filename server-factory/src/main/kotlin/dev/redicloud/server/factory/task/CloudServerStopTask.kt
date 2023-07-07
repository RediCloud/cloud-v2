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
        var responded = 0
        var total = 0
        serverFactory.stopQueue.forEach {
            total++
            defaultScope.launch {
                try {
                    val server = serverRepository.getServer<CloudServer>(it)
                    if (server == null) {
                        serverFactory.stopQueue.remove(it)
                        responded++
                        return@launch
                    }
                    if (server.hostNodeId == serviceId) {
                        serverFactory.stopQueue.remove(it)
                        defaultScope.launch {
                            serverFactory.stopServer(it)
                        }
                    }
                }catch (e: Exception) {
                    logger.severe("Failed to stop server ${it.toName()}", e)
                }finally {
                    responded++
                }
            }
        }
        while (responded != total) {
            Thread.sleep(333)
        }
        return false
    }
}