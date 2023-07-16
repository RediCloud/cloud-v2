package dev.redicloud.repository.server

import dev.redicloud.api.service.server.CloudServerState
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
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class ServerRepository(
    databaseConnection: DatabaseConnection,
    serviceId: ServiceId,
    packetManager: PacketManager,
    private val eventManager: EventManager,
    configurationTemplateRepository: ConfigurationTemplateRepository
) : CachedServiceRepository<CloudServer>(
    databaseConnection,
    serviceId,
    if (serviceId.type.isServer()) serviceId.type else ServiceType.MINECRAFT_SERVER,
    packetManager,
    arrayOf(CloudMinecraftServer::class, CloudProxyServer::class),
    5.minutes,
    ServiceType.NODE,
    ServiceType.MINECRAFT_SERVER,
    ServiceType.PROXY_SERVER
) {

    suspend fun <T : CloudServer> existsServer(serviceId: ServiceId): Boolean {
        return existsService<T>(serviceId)
    }

    suspend fun <T : CloudServer> getServer(serviceId: ServiceId): T? =
        getService(serviceId)

    suspend fun <T : CloudServer> getServer(name: String, type: ServiceType): T? =
        getRegisteredServices().filter { it.serviceId.type == type }
            .firstOrNull { it.name.lowercase() == name.lowercase() } as? T

    suspend fun getMinecraftServer(serviceId: ServiceId): CloudMinecraftServer? =
        getServer(serviceId)

    suspend fun getProxyServer(serviceId: ServiceId): CloudProxyServer? =
        getServer(serviceId)

    suspend fun <T : CloudServer> updateServer(cloudServer: T): T {
        val oldServer = getServer<T>(cloudServer.serviceId)
        updateService(cloudServer)
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

    suspend fun <T : CloudServer> createServer(cloudServer: T): T {
        createService(cloudServer)
        eventManager.fireEvent(CloudServerRegisteredEvent(cloudServer.serviceId))
        if (cloudServer.state != CloudServerState.UNKNOWN) {
            eventManager.fireEvent(CloudServerStateChangeEvent(cloudServer.serviceId, cloudServer.state))
        }
        return cloudServer
    }


    suspend fun <T : CloudServer> deleteServer(cloudServer: T) {
        val unregister = getRegisteredIds().contains(cloudServer.serviceId)
        deleteService(cloudServer)
        if (unregister) eventManager.fireEvent(CloudServerUnregisteredEvent(cloudServer.serviceId))
    }

    suspend fun getConnectedServers(): List<CloudServer> =
        getConnectedServices().filter { it.serviceId.type.isServer() }.toList() as List<CloudServer>

    suspend fun <T : CloudServer> getConnectedServers(type: ServiceType): List<T> =
        getConnectedIds().filter { it.type == type }.mapNotNull { getServer(it) }

    suspend fun getRegisteredServers(): List<CloudServer> =
        getRegisteredServices().filter { it.serviceId.type.isServer() }.toList() as List<CloudServer>

    suspend fun <T : CloudServer> getRegisteredServers(type: ServiceType): List<T> =
        getRegisteredIds().filter { it.type == type }.mapNotNull { getServer(it) }

    override suspend fun transformShutdownable(service: CloudServer): CloudServer {
        service.state = CloudServerState.STOPPING
        return service
    }

    suspend fun getFallback(vararg currentServerIds: ServiceId?): CloudMinecraftServer? {
        return getConnectedServers<CloudMinecraftServer>(ServiceType.MINECRAFT_SERVER)
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