package dev.redicloud.repository.server

import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.repository.service.ServiceSessions
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.utils.service.ServiceId

class CloudMinecraftServer(
    serviceId: ServiceId,
    configurationTemplate: ConfigurationTemplate,
    id: Int,
    hostNodeId: ServiceId,
    serviceSessions: ServiceSessions,
    hidden: Boolean,
    state: CloudServerState = CloudServerState.UNKNOWN,
    port: Int,
    maxPlayers: Int
) : CloudServer(serviceId, configurationTemplate, id, hostNodeId, serviceSessions, hidden, state, port, maxPlayers, mutableListOf())