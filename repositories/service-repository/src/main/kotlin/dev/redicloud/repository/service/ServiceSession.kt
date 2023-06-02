package dev.redicloud.repository.service

import dev.redicloud.utils.ServiceId
import java.util.*

data class ServiceSession(
    val serviceId: ServiceId,
    val startTime: Long,
    var endTime: Long = -1L,
    val sessionId: UUID = UUID.randomUUID(),
    val ipAddress: String
)