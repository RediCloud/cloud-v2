package dev.redicloud.service.minecraft

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.base.repository.BaseFileTemplateRepository
import dev.redicloud.service.minecraft.repositories.connect
import dev.redicloud.service.minecraft.repositories.disconnect
import dev.redicloud.utils.DATABASE_JSON
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.runBlocking

abstract class MinecraftServerService<T> : BaseService(
    DatabaseConfiguration.fromFile(DATABASE_JSON.getFile()),
    null,
    ServiceId.fromString(System.getenv("RC_SERVICE_ID"))
) {


    final override val fileTemplateRepository: AbstractFileTemplateRepository
    final override val serverVersionTypeRepository: CloudServerVersionTypeRepository
    final override val serverRepository: ServerRepository
    val logger = LogManager.Companion.logger("BukkitConnector")

    init {
        serverRepository = ServerRepository(this.databaseConnection, this.serviceId, this.packetManager, this.eventManager)
        fileTemplateRepository = BaseFileTemplateRepository(this.databaseConnection, this.nodeRepository)
        serverVersionTypeRepository = CloudServerVersionTypeRepository(this.databaseConnection, null)

        runBlocking {
            registerDefaults()
            serverRepository.connect(serviceId)
        }
    }

    open fun onEnable() = runBlocking {
        try {
            val server = serverRepository.getServer<CloudServer>(serviceId) ?: throw IllegalStateException("Server not found!")
            server.state = CloudServerState.RUNNING
            serverRepository.updateServer(server)
            logger.info("Enabled cloud connector for server ${server.getIdentifyingName(false)}!")
        } catch (e: Exception) {
            logger.severe("Failed to enable cloud connector! Stopping server...", e)
            shutdown()
        }
    }

    open fun onDisable() {
        shutdown()
    }

    override fun shutdown() {
        if (SHUTTINGDOWN) return
        SHUTTINGDOWN = true
        runBlocking {
            serverRepository.disconnect(serviceId)
        }
        super.shutdown()
    }

    abstract fun getConnectorPlugin(): T
}