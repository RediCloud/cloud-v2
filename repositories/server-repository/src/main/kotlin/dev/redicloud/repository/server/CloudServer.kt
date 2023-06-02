package dev.redicloud.repository.server

import dev.redicloud.repository.service.CloudService
import dev.redicloud.repository.service.ServiceSession
import dev.redicloud.utils.ServiceId

class CloudServer(
    serviceId: ServiceId,
    sessions: MutableList<ServiceSession>
) : CloudService(serviceId, sessions)