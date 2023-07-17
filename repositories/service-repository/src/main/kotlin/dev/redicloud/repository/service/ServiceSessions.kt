package dev.redicloud.repository.service

import dev.redicloud.api.repositories.service.ICloudServiceSession
import dev.redicloud.utils.service.ServiceId
import java.util.*

class ServiceSessions {
    var currentSession: ServiceSession? = null
    var registrationSession: ServiceSession? = null
    val sessionHistory: MutableList<ServiceSession> = mutableListOf()
}

class ServiceSession(
    override val serviceId: ServiceId,
    override val startTime: Long,
    override val ipAddress: String
) : ICloudServiceSession {

    override val sessionId: UUID = UUID.randomUUID()
    override var endTime: Long = -1L
    override var suspended: Boolean = false

}