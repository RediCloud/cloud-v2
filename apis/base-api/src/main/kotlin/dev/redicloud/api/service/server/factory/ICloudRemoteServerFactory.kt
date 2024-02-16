package dev.redicloud.api.service.server.factory

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplate
import dev.redicloud.api.utils.factory.ServerQueueInformation
import dev.redicloud.api.utils.factory.TransferServerQueueInformation
import java.util.UUID

interface ICloudRemoteServerFactory {

    suspend fun queueStart(configurationTemplate: ICloudConfigurationTemplate, count: Int = 1): List<UUID>

    suspend fun queueStart(serverId: ServiceId)

    suspend fun queueStop(serverId: ServiceId, force: Boolean = false): Boolean

    suspend fun queueDelete(serverId: ServiceId)

    suspend fun queueTransfer(serverId: ServiceId, targetNodeId: ServiceId)

    suspend fun getStartingQueue(): List<ServerQueueInformation>

    suspend fun getStoppingQueue(): List<ServiceId>

    suspend fun getDeletingQueue(): List<ServiceId>

    suspend fun getTransferQueue(): List<TransferServerQueueInformation>

}