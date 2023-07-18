package dev.redicloud.repository.server

import dev.redicloud.api.events.impl.server.CloudServerConnectedEvent
import dev.redicloud.api.events.impl.server.CloudServerDisconnectedEvent
import dev.redicloud.api.events.impl.server.CloudServerUnregisteredEvent
import dev.redicloud.api.events.impl.template.configuration.ConfigurationTemplateUpdateEvent
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.event.EventManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.service.CachedServiceRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.api.events.impl.server.CloudServerRegisteredEvent
import dev.redicloud.api.events.impl.server.CloudServerStateChangeEvent
import dev.redicloud.api.events.listen
import dev.redicloud.api.service.server.*
import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.utils.defaultScope
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class ServerRepository(
    databaseConnection: DatabaseConnection,
    private val serviceId: ServiceId,
    packetManager: PacketManager,
    private val eventManager: EventManager,
    configurationTemplateRepository: ConfigurationTemplateRepository
) : ServiceRepository (
    databaseConnection,
    packetManager
) , ICloudServerRepository {

    val internalMinecraftServerRepository = InternalServerRepository(
        databaseConnection,
        packetManager,
        ICloudMinecraftServer::class,
        CloudMinecraftServer::class,
        ServiceType.MINECRAFT_SERVER,
        this
    ).apply { internalRepositories.add(this) }
    val internalProxyServerRepository = InternalServerRepository(
        databaseConnection,
        packetManager,
        ICloudProxyServer::class,
        CloudProxyServer::class,
        ServiceType.PROXY_SERVER,
        this
    ).apply { internalRepositories.add(this) }

    override suspend fun <T : ICloudServer> existsServer(serviceId: ServiceId): Boolean {
        return when (serviceId.type) {
            ServiceType.MINECRAFT_SERVER -> internalMinecraftServerRepository.existsService(serviceId)
            ServiceType.PROXY_SERVER -> internalProxyServerRepository.existsService(serviceId)
            else -> throw IllegalArgumentException("Unknown service type ${serviceId.type} (${serviceId.type})")
        }
    }

    override suspend fun <T : ICloudServer> getServer(serviceId: ServiceId): T? {
        return when (serviceId.type) {
            ServiceType.MINECRAFT_SERVER -> internalMinecraftServerRepository.getService(serviceId) as T?
            ServiceType.PROXY_SERVER -> internalProxyServerRepository.getService(serviceId) as T?
            else -> throw IllegalArgumentException("Unknown service type ${serviceId.type} (${serviceId.type})")
        }
    }

    override suspend fun <T : ICloudServer> getServer(name: String, type: ServiceType): T? {
        return when (type) {
            ServiceType.MINECRAFT_SERVER -> internalMinecraftServerRepository.getService(name) as T?
            ServiceType.PROXY_SERVER -> internalProxyServerRepository.getService(name) as T?
            else -> throw IllegalArgumentException("Unknown service type $type ($type)")
        }
    }

    override suspend fun getMinecraftServer(serviceId: ServiceId): CloudMinecraftServer? {
        return internalMinecraftServerRepository.getService(serviceId)
    }

    override suspend fun getProxyServer(serviceId: ServiceId): CloudProxyServer? {
        return internalProxyServerRepository.getService(serviceId)
    }

    override suspend fun <T : ICloudServer> updateServer(cloudServer: T): T {
        val oldServer = getServer<T>(cloudServer.serviceId)
        when (cloudServer) {
            is CloudMinecraftServer -> internalMinecraftServerRepository.updateService(cloudServer as CloudMinecraftServer)
            is CloudProxyServer -> internalProxyServerRepository.updateService(cloudServer as CloudProxyServer)
            else -> throw IllegalArgumentException("Unknown service type ${cloudServer.serviceId.type} (${cloudServer.serviceId.type})")
        }
        if (oldServer?.state != cloudServer.state) {
            eventManager.fireEvent(CloudServerStateChangeEvent(cloudServer.serviceId, cloudServer.state))
        }

        if (oldServer != null && (oldServer.connected != cloudServer.connected || (oldServer.state != cloudServer.state && cloudServer.state == CloudServerState.STOPPED))) {
            if (cloudServer.connected) {
                eventManager.fireEvent(CloudServerConnectedEvent(cloudServer.serviceId))
            } else if (cloudServer.state == CloudServerState.STOPPED) {
                eventManager.fireEvent(CloudServerDisconnectedEvent(cloudServer.serviceId))
            }
        }
        return cloudServer
    }

    suspend fun <T : ICloudServer> createServer(cloudServer: T): T {
        when (cloudServer) {
            is CloudMinecraftServer -> internalMinecraftServerRepository.createService(cloudServer as CloudMinecraftServer)
            is CloudProxyServer -> internalProxyServerRepository.createService(cloudServer as CloudProxyServer)
            else -> throw IllegalArgumentException("Unknown service type ${cloudServer.serviceId.type} (${cloudServer.serviceId.type})")
        }
        eventManager.fireEvent(CloudServerRegisteredEvent(cloudServer.serviceId))
        if (cloudServer.state != CloudServerState.UNKNOWN) {
            eventManager.fireEvent(CloudServerStateChangeEvent(cloudServer.serviceId, cloudServer.state))
        }
        return cloudServer
    }


    suspend fun <T : ICloudServer> deleteServer(cloudServer: T) {
        val unregister = registeredServices.contains(cloudServer.serviceId)
        when (cloudServer) {
            is CloudMinecraftServer -> internalMinecraftServerRepository.deleteService(cloudServer as CloudMinecraftServer)
            is CloudProxyServer -> internalProxyServerRepository.deleteService(cloudServer as CloudProxyServer)
            else -> throw IllegalArgumentException("Unknown service type ${cloudServer.serviceId.type} (${cloudServer.serviceId.type})")
        }
        if (unregister) eventManager.fireEvent(CloudServerUnregisteredEvent(cloudServer.serviceId))
    }

    override suspend fun getConnectedServers(): List<CloudServer> =
        connectedServices.filter { it.type.isServer() }.mapNotNull { getServer(it) }

    override suspend fun <T : ICloudServer> getConnectedServers(type: ServiceType): List<T> =
        connectedServices.filter { it.type == type }.mapNotNull { getServer(it) }

    override suspend fun getRegisteredServers(): List<CloudServer> =
        registeredServices.filter { it.type.isServer() }.mapNotNull { getServer(it) }

    override suspend fun <T : ICloudServer> getRegisteredServers(type: ServiceType): List<T> =
        registeredServices.filter { it.type == type }.mapNotNull { getServer(it) }

    override suspend fun getFallback(vararg currentServerIds: ServiceId?): CloudMinecraftServer? {
        return getConnectedServers<CloudMinecraftServer>(ServiceType.MINECRAFT_SERVER)
            .asSequence()
            .filter { it.serviceId != serviceId }
            .filter { !currentServerIds.toList().contains(it.serviceId) }
            //TODO check permissions
            .filter { it.configurationTemplate.fallbackServer }
            .filter { it.state == CloudServerState.RUNNING }
            .filter { it.connectedPlayers.size < it.maxPlayers || it.maxPlayers == -1 }
            .minByOrNull { it.connectedPlayers.size }
    }

    private val onConfigurationTemplateUpdateEvent = eventManager.listen<ConfigurationTemplateUpdateEvent> {
        defaultScope.launch {
            val configurationTemplate = configurationTemplateRepository.getTemplate(it.configurationTemplateId) ?: return@launch
            getRegisteredServers()
                .filter { it.configurationTemplate.uniqueId == configurationTemplate.uniqueId }.forEach {
                    if (it.state == CloudServerState.STOPPED) {
                        it.configurationTemplate = configurationTemplate
                    }else {
                        it.configurationTemplate.fallbackServer = configurationTemplate.fallbackServer
                        it.configurationTemplate.joinPermission = configurationTemplate.joinPermission
                        it.configurationTemplate.maxPlayers = configurationTemplate.maxPlayers
                        it.configurationTemplate.percentToStartNewService = configurationTemplate.percentToStartNewService
                        it.configurationTemplate.startPriority = configurationTemplate.startPriority
                    }
                    updateServer(it)
                }
        }
    }

}