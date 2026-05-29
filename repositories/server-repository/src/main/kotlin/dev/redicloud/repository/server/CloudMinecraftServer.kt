package dev.redicloud.repository.server

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.service.server.ICloudMinecraftServer
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplate
import dev.redicloud.repository.service.ServiceSessions

class CloudMinecraftServer(
    serviceId: ServiceId,
    configurationTemplate: ICloudConfigurationTemplate,
    id: Int,
    hostNodeId: ServiceId,
    serviceSessions: ServiceSessions,
    hidden: Boolean,
    state: CloudServerState,
    port: Int,
    maxPlayers: Int
) : CloudServer(
    serviceId,
    configurationTemplate,
    id,
    hostNodeId,
    serviceSessions,
    hidden,
    state,
    port,
    maxPlayers,
    mutableListOf()
),
    ICloudMinecraftServer
