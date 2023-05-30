package dev.redicloud.cluster.service

import dev.redicloud.utils.ServiceId
import java.util.*

data class ServiceClusterSession(
    val serviceId: ServiceId,
    val sessionStartTime: Long,
    val sessionId: UUID = UUID.randomUUID()
)