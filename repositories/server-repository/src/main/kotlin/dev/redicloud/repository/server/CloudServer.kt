package dev.redicloud.repository.server

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.repository.service.CloudService
import dev.redicloud.repository.service.ServiceSession
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.utils.service.ServiceId

abstract class CloudServer(
    serviceId: ServiceId,
    val configurationTemplate: ConfigurationTemplate,
    val id: Int,
    val hostNodeId: ServiceId,
    sessions: MutableList<ServiceSession>,
    var hidden: Boolean,
    var state: CloudServerState = CloudServerState.UNKNOWN,
    var port: Int = -1,
) : CloudService(serviceId, "${configurationTemplate.name}${configurationTemplate.serverSplitter}$id", sessions) {

    override fun unregisterAfterDisconnect(): Boolean {
        return super.unregisterAfterDisconnect() && !configurationTemplate.static
    }

}