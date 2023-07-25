package dev.redicloud.service.minecraft

import dev.redicloud.api.service.ServiceId
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.listener.CloudServerListener
import dev.redicloud.api.service.ServiceType
import dev.redicloud.utils.coroutineExceptionHandler
import kotlinx.coroutines.runBlocking

abstract class ProxyServerService<T> : MinecraftServerService<T>() {

    init {
        runBlocking(coroutineExceptionHandler) {
            registerListeners()
        }
    }

    abstract fun registerServer(server: CloudMinecraftServer)

    abstract fun unregisterServer(serviceId: ServiceId)

    private fun registerListeners() {
        this.eventManager.registerListener(CloudServerListener(this))
    }

    protected suspend fun registerStartedServers() {
        this.serverRepository.getConnectedServers<CloudMinecraftServer>(ServiceType.MINECRAFT_SERVER).forEach {
            registerServer(it)
        }
    }

}