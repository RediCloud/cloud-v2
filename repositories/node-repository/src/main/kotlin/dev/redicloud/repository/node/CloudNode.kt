package dev.redicloud.repository.node

import dev.redicloud.repository.service.CloudService
import dev.redicloud.repository.service.ServiceSessions
import dev.redicloud.utils.service.ServiceId

class CloudNode(
    serviceId: ServiceId,
    name: String,
    serviceSessions: ServiceSessions,
    val hostedServers: MutableList<ServiceId>,
    var master: Boolean,
    var currentMemoryUsage: Long,
    var maxMemory: Long
) : CloudService(serviceId, name, serviceSessions)