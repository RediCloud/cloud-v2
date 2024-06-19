package dev.redicloud.server.factory.task

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.logging.LogManager
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.MultiAsyncAction

class CloudServerUnregisterTask(
    private val thisNodeId: ServiceId,
    private val serverFactory: ServerFactory,
    private val nodeRepository: ICloudNodeRepository,
    private val serverRepository: ICloudServerRepository
) : CloudTask() {

    companion object {
        private val LOGGER = LogManager.logger(CloudServerUnregisterTask::class)
    }

    override suspend fun execute(): Boolean {
        if (nodeRepository.getMasterNode()?.serviceId != thisNodeId) {
            return false
        }
        val actions = MultiAsyncAction()
        serverFactory.unregisterQueue.forEach { serviceId ->
            serverFactory.unregisterQueue.remove(serviceId)
            if (!serverRepository.existsServer<ICloudServer>(serviceId)) {
                return@forEach
            }
            actions.add {
                try {
                    serverFactory.unregisterServer(serviceId, force = true)
                }catch (e: Exception) {
                    LOGGER.severe("§cFailed to unregister server ${serviceId.toName()}!", e)
                }
            }
        }

        actions.joinAll()

        return false
    }

}