package dev.redicloud.api.service

import java.util.*

interface ICloudServiceSession {
    val serviceId: ServiceId
    val startTime: Long
    val ipAddress: String
    val endTime: Long
    val sessionId: UUID
    val suspended: Boolean
}