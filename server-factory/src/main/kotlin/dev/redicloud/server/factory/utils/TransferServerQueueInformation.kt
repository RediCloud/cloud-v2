package dev.redicloud.server.factory.utils

import dev.redicloud.utils.service.ServiceId

data class TransferServerQueueInformation(
    val serverId: ServiceId,
    val targetNodeId: ServiceId
)