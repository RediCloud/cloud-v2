package dev.redicloud.service.minecraft

import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.listener.CloudServerListener
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.runBlocking

abstract class ProxyServerService<T> : MinecraftServerService<T>() {

    init {
        runBlocking {
            registerListeners()
        }
    }

    abstract fun registerServer(server: CloudMinecraftServer)

    abstract fun unregisterServer(server: CloudMinecraftServer)

    private fun registerListeners() {
        this.eventManager.register(CloudServerListener(this))
    }

    protected suspend fun registerStartedServers() {
        this.serverRepository.getConnectedServers<CloudMinecraftServer>(ServiceType.MINECRAFT_SERVER).forEach {
            registerServer(it)
        }
    }

}