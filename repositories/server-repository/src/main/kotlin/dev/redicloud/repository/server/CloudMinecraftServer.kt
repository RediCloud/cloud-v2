package dev.redicloud.repository.server

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.repository.service.ServiceSession
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.utils.service.ServiceId

class CloudMinecraftServer(
    serviceId: ServiceId,
    configurationTemplate: ConfigurationTemplate,
    id: Int,
    hostNodeId: ServiceId,
    sessions: MutableList<ServiceSession>,
    hidden: Boolean,
    state: CloudServerState = CloudServerState.UNKNOWN,
    port: Int,
    maxPlayers: Int
) : CloudServer(serviceId, configurationTemplate, id, hostNodeId, sessions, hidden, state, port, maxPlayers, mutableListOf())