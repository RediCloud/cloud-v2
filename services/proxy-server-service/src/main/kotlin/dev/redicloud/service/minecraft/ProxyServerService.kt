package dev.redicloud.service.minecraft

import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.listener.CloudServerListener

abstract class ProxyServerService<T> : MinecraftServerService<T>() {

    init {
        this.registerListeners()
    }

    abstract fun registerServer(server: CloudMinecraftServer)

    abstract fun unregisterServer(server: CloudMinecraftServer)

    private fun registerListeners() {
        this.eventManager.register(CloudServerListener(this))
    }

}