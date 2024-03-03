package dev.redicloud.connector.minestom

import dev.redicloud.api.provider.IServerPlayerProvider
import dev.redicloud.connector.minestom.provider.MinestomScreenProvider
import dev.redicloud.connector.minestom.provider.MinestomServerPlayerProvider
import dev.redicloud.service.minecraft.MinecraftServerService
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import net.minestom.server.extensions.Extension

class MinestomConnector(val extension: Extension) : MinecraftServerService<Extension>() {

    internal var minestomShuttingDown = false
    override val screenProvider: AbstractScreenProvider
        = MinestomScreenProvider(this.packetManager, this.extension)
    override var playerProvider: IServerPlayerProvider
        = MinestomServerPlayerProvider()

    init {
        initApi()
        registerTasks()
        runBlocking { moduleHandler.loadModules() }
    }

    override fun onDisable() {
        if (!this.minestomShuttingDown) {
            MinecraftServer.stopCleanly()
            return
        }
        super.onDisable()
    }

    override fun plattformShutdown() {
        this.minestomShuttingDown = true
        MinecraftServer.stopCleanly()
    }

    override fun getConnectorPlugin(): Extension = this.extension

}