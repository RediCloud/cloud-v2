package dev.redicloud.service.minecraft

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.listener.AbstractCloudNotificationListeners
import dev.redicloud.service.minecraft.listener.CloudServerListener

abstract class ProxyServerService<T, S> : MinecraftServerService<T>() {

    protected val registeredServers: MutableMap<ServiceId, S> = mutableMapOf()
    abstract val notificationListeners: AbstractCloudNotificationListeners

    override suspend fun start() {
        super.start()
        registerListeners()
    }

    abstract fun registerServer(server: CloudMinecraftServer)

    abstract fun unregisterServer(serviceId: ServiceId)

    fun isServerRegistered(serviceId: ServiceId): Boolean {
        return this.registeredServers.containsKey(serviceId)
    }

    private fun registerListeners() {
        this.eventManager.registerListener(CloudServerListener(this))
    }

    protected suspend fun registerStartedServers() {
        this.serverRepository.getConnectedServers<CloudMinecraftServer>(ServiceType.MINECRAFT_SERVER).forEach {
            if (isServerRegistered(it.serviceId)) return@forEach
            registerServer(it)
        }
    }
}
