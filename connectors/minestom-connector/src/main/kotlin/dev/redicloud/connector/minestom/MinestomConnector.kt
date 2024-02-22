package dev.redicloud.connector.minestom

import dev.redicloud.connector.minestom.provider.MinestomScreenProvider
import dev.redicloud.connector.minestom.provider.MinestomServerPlayerProvider
import dev.redicloud.service.minecraft.MinecraftServerService
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import dev.redicloud.service.minecraft.provider.IServerPlayerProvider
import kotlinx.coroutines.runBlocking
import net.minestom.server.extensions.Extension

class MinestomConnector(val extension: Extension) : MinecraftServerService<Extension>() {

    internal var minestomShuttingDown = false
    override val screenProvider: AbstractScreenProvider
        = MinestomScreenProvider(this.packetManager, this.extension)
    override val serverPlayerProvider: IServerPlayerProvider
        = MinestomServerPlayerProvider()

    init {
        initApi()
        registerTasks()
        runBlocking { moduleHandler.loadModules() }
    }

    override fun getConnectorPlugin(): Extension = this.extension

}