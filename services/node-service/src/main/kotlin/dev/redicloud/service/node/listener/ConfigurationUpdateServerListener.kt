package dev.redicloud.service.node.listener

import dev.redicloud.api.events.IEventManager
import dev.redicloud.api.events.impl.template.configuration.ConfigurationTemplateUpdateEvent
import dev.redicloud.api.events.listen
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplateRepository
import dev.redicloud.event.EventManager
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.service.node.repository.node.LOGGER
import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.launch

class ConfigurationUpdateServerListener(
    serviceId: ServiceId,
    eventManager: EventManager,
    configurationTemplateRepository: ICloudConfigurationTemplateRepository,
    serverRepository: ICloudServerRepository,
    nodeRepository: ICloudNodeRepository
) {

    private val onConfigurationTemplateUpdateEvent = eventManager.listen<ConfigurationTemplateUpdateEvent> {
        defaultScope.launch {
            val thisNode = nodeRepository.getNode(serviceId) ?: return@launch
            if (!thisNode.master) {
                return@launch
            }
            LOGGER.info("Updating server configuration templates")
            val configurationTemplate =
                configurationTemplateRepository.getTemplate(it.configurationTemplateId) ?: return@launch
            serverRepository.getRegisteredServers()
                .filter { it.configurationTemplate.uniqueId == configurationTemplate.uniqueId }.forEach {
                    if (it.state == CloudServerState.STOPPED) {
                        (it as CloudServer).configurationTemplate = configurationTemplate
                    } else {
                        it.configurationTemplate.fallbackServer = configurationTemplate.fallbackServer
                        it.configurationTemplate.joinPermission = configurationTemplate.joinPermission
                        it.configurationTemplate.maxPlayers = configurationTemplate.maxPlayers
                        it.configurationTemplate.percentToStartNewService =
                            configurationTemplate.percentToStartNewService
                        it.configurationTemplate.startPriority = configurationTemplate.startPriority
                    }
                    serverRepository.updateServer(it)
                }
        }
    }

}