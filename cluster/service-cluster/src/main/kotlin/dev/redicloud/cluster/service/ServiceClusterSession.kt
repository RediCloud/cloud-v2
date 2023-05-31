package dev.redicloud.cluster.service

import dev.redicloud.utils.ServiceId
import java.util.*

data class ServiceClusterSession(
    val serviceId: ServiceId,
    val startTime: Long,
    var endTime: Long = -1L,
    val sessionId: UUID = UUID.randomUUID()
)