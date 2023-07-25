package dev.redicloud.service.minecraft.listener

import dev.redicloud.api.events.impl.server.CloudServerConnectedEvent
import dev.redicloud.api.events.impl.server.CloudServerDisconnectedEvent
import dev.redicloud.api.events.listen
import dev.redicloud.service.minecraft.ProxyServerService
import dev.redicloud.utils.defaultScope
import dev.redicloud.api.service.ServiceType
import kotlinx.coroutines.launch

class CloudServerListener(
    private val proxyServerService: ProxyServerService<*>
) {

    private val onServerConnectEvent = proxyServerService.eventManager.listen<CloudServerConnectedEvent> {
        defaultScope.launch {
            if (it.serviceId.type != ServiceType.MINECRAFT_SERVER) return@launch
            val server = proxyServerService.serverRepository.getMinecraftServer(it.serviceId)
                ?: throw IllegalStateException("Cant register server that is not in the repository: ${it.serviceId.toName()}")
            proxyServerService.registerServer(server)
        }
    }

    private val onServerDisconnectEvent = proxyServerService.eventManager.listen<CloudServerDisconnectedEvent> {
        if (it.serviceId.type != ServiceType.MINECRAFT_SERVER) return@listen
        proxyServerService.unregisterServer(it.serviceId)
    }

}