package dev.redicloud.repository.node

import dev.redicloud.repository.service.CloudService
import dev.redicloud.repository.service.ServiceSession
import dev.redicloud.utils.ServiceId

class CloudNode(
    serviceId: ServiceId,
    sessions: MutableList<ServiceSession>,
    private val hostedServers: MutableList<ServiceId>
) : CloudService(serviceId, sessions) {

    fun getHostedServers(): List<ServiceId> = hostedServers.toList()

}