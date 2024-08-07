package dev.redicloud.server.factory

import dev.redicloud.api.database.grid.list.ISyncedMutableList
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.service.server.factory.ICloudRemoteServerFactory
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplate
import dev.redicloud.api.utils.factory.FailedStarts
import dev.redicloud.api.utils.factory.ServerQueueInformation
import dev.redicloud.api.utils.factory.TransferServerQueueInformation
import dev.redicloud.api.utils.factory.calculateStartOrder
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import java.util.*

open class RemoteServerFactory(
    databaseConnection: DatabaseConnection,
    private val nodeRepository: NodeRepository,
    private val serverRepository: ServerRepository
) : ICloudRemoteServerFactory {

    companion object {
        private val logger = LogManager.logger(RemoteServerFactory::class)
    }

    val hostingId = databaseConnection.serviceId

    val startQueue: ISyncedMutableList<ServerQueueInformation> =
        databaseConnection.getMutableList("server-factory:queue:start")
    val stopQueue: ISyncedMutableList<ServiceId> =
        databaseConnection.getMutableList("server-factory:queue:stop")
    val deleteQueue: ISyncedMutableList<ServiceId> =
        databaseConnection.getMutableList("server-factory:queue:delete")
    val transferQueue: ISyncedMutableList<TransferServerQueueInformation> =
        databaseConnection.getMutableList("server-factory:queue:transfer")
    val unregisterQueue: ISyncedMutableList<ServiceId> =
        databaseConnection.getMutableList("server-factory:queue:unregister")
    var shutdown = false

    override suspend fun queueStart(configurationTemplate: ICloudConfigurationTemplate, count: Int, targetNodeId: ServiceId?): List<UUID> {
        val info = ServerQueueInformation(UUID.randomUUID(), configurationTemplate, null, queueTime = System.currentTimeMillis())
        val nodes = nodeRepository.getRegisteredNodes()
        info.calculateStartOrder(nodes, serverRepository)
        val ids = mutableListOf<UUID>()
        for (i in 1..count) {
            val clone = ServerQueueInformation(
                UUID.randomUUID(),
                configurationTemplate,
                null,
                info.failedStarts,
                info.nodeStartOrder,
                targetNodeId,
                System.currentTimeMillis()
            )
            startQueue.add(clone)
            ids.add(clone.uniqueId)
        }
        return ids
    }

    override suspend fun queueStart(serverId: ServiceId) {
        val server = serverRepository.getServer<CloudServer>(serverId) ?: throw IllegalArgumentException("Server with id $serverId not found")
        val info = ServerQueueInformation(UUID.randomUUID(), server.configurationTemplate, serverId, queueTime = System.currentTimeMillis())
        val nodes = nodeRepository.getRegisteredNodes()
        info.calculateStartOrder(nodes, serverRepository)
        startQueue.add(info)
    }

    override suspend fun queueStop(serverId: ServiceId, force: Boolean): Boolean {
        val server = serverRepository.getServer<CloudServer>(serverId) ?: return false
        if (server.state == CloudServerState.STOPPED || server.state == CloudServerState.STOPPING && !force) return false
        stopQueue.add(serverId)
        return true
    }

    override suspend fun queueDelete(serverId: ServiceId) {
        deleteQueue.add(serverId)
    }

    override suspend fun queueUnregister(serviceId: ServiceId) {
        unregisterQueue.add(serviceId)
    }

    override suspend fun queueTransfer(serverId: ServiceId, targetNodeId: ServiceId) {
        transferQueue.add(TransferServerQueueInformation(serverId, targetNodeId))
    }

    override suspend fun getStartingQueue(): List<ServerQueueInformation> {
        return startQueue.toList()
    }

    override suspend fun getStoppingQueue(): List<ServiceId> {
        return stopQueue.toList()
    }

    override suspend fun getDeletingQueue(): List<ServiceId> {
        return deleteQueue.toList()
    }

    override suspend fun getTransferQueue(): List<TransferServerQueueInformation> {
        return transferQueue.toList()
    }

    override suspend fun getUnregisterQueue(): List<ServiceId> {
        return unregisterQueue.toList()
    }

}