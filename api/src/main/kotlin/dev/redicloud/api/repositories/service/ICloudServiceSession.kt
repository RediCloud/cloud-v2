package dev.redicloud.api.repositories.service

import dev.redicloud.utils.service.ServiceId
import java.util.*

interface ICloudServiceSession {
    val serviceId: ServiceId
    val startTime: Long
    val ipAddress: String
    val endTime: Long
    val sessionId: UUID
    val suspended: Boolean
}