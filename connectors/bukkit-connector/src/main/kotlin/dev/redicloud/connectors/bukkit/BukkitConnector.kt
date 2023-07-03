package dev.redicloud.connectors.bukkit

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.connectors.bukkit.repositories.connect
import dev.redicloud.connectors.bukkit.repositories.disconnect
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.base.repository.BaseFileTemplateRepository
import dev.redicloud.utils.DATABASE_JSON
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

class BukkitConnector(val plugin: JavaPlugin) : BaseService(
    DatabaseConfiguration.fromFile(DATABASE_JSON.getFile()),
    null,
    ServiceId.fromString(System.getenv("RC_SERVICE_ID"))
) {

    override val fileTemplateRepository: AbstractFileTemplateRepository
    override val serverVersionTypeRepository: CloudServerVersionTypeRepository
    val logger = LogManager.Companion.logger("BukkitConnector")

    init {
        fileTemplateRepository = BaseFileTemplateRepository(this.databaseConnection, this.nodeRepository)
        serverVersionTypeRepository = CloudServerVersionTypeRepository(this.databaseConnection, null)

        runBlocking {
            registerDefaults()
            serverRepository.connect(serviceId)
        }
    }

    fun onEnable() = runBlocking {
        try {
            val server = serverRepository.getServer(serviceId) ?: throw IllegalStateException("Server not found!")
            server.state = CloudServerState.RUNNING
            serverRepository.updateServer(server)
            logger.info("Enabled cloud connector for server ${server.getIdentifyingName(false)}!")
        } catch (e: Exception) {
            logger.severe("Failed to enable cloud connector! Stopping server...", e)
            shutdown()
        }
    }

    override fun shutdown() {
        if (SHUTTINGDOWN) return
        SHUTTINGDOWN = true
        runBlocking {
            serverRepository.disconnect(serviceId)
        }
        super.shutdown()
    }

}