package dev.redicloud.api.utils.factory

import dev.redicloud.api.service.ServiceId

data class TransferServerQueueInformation(
    val serverId: ServiceId,
    val targetNodeId: ServiceId
)