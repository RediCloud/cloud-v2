package dev.redicloud.repository.server

import dev.redicloud.repository.service.CloudService
import dev.redicloud.repository.service.ServiceSession
import dev.redicloud.repository.temlate.configuration.ConfigurationTemplate
import dev.redicloud.utils.service.ServiceId

class CloudServer(
    serviceId: ServiceId,
    val configurationTemplate: ConfigurationTemplate,
    val id: Int,
    sessions: MutableList<ServiceSession>
) : CloudService(serviceId, "${configurationTemplate.name}${configurationTemplate.serverSplitter}$id", sessions)