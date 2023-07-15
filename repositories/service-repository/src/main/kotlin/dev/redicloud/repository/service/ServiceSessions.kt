package dev.redicloud.repository.service

import dev.redicloud.utils.service.ServiceId
import java.util.*

data class ServiceSessions(
    var registrationSession: ServiceSession? = null,
    var currentSession: ServiceSession? = null,
    val sessionHistory: MutableList<ServiceSession> = mutableListOf()
)

data class ServiceSession(
    val serviceId: ServiceId,
    val startTime: Long,
    val ipAddress: String,
    var endTime: Long = -1L,
    val sessionId: UUID = UUID.randomUUID(),
    var suspended: Boolean = false
)