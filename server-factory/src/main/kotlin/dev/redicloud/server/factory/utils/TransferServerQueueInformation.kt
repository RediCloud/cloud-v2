package dev.redicloud.server.factory.utils

import dev.redicloud.api.service.ServiceId

data class TransferServerQueueInformation(
    val serverId: ServiceId,
    val targetNodeId: ServiceId
)