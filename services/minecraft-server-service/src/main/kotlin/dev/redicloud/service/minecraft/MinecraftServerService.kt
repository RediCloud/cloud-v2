package dev.redicloud.service.minecraft

import com.google.inject.name.Names
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.template.file.ICloudFileTemplateRepository
import dev.redicloud.api.version.ICloudServerVersionTypeRepository
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.base.repository.BaseFileTemplateRepository
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import dev.redicloud.service.minecraft.provider.IServerPlayerProvider
import dev.redicloud.service.minecraft.repositories.connect
import dev.redicloud.service.minecraft.tasks.CloudServerInfoTask
import dev.redicloud.api.utils.DATABASE_JSON
import dev.redicloud.api.service.ServiceId
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

abstract class MinecraftServerService<T> : BaseService(
    DatabaseConfiguration.fromFile(DATABASE_JSON.getFile()),
    null,
    ServiceId.fromString(System.getenv("RC_SERVICE_ID"))
) {


    private val hostServiceId: ServiceId
    final override val fileTemplateRepository: AbstractFileTemplateRepository
    final override val serverVersionTypeRepository: CloudServerVersionTypeRepository
    abstract val serverPlayerProvider: IServerPlayerProvider
    abstract val screenProvider: AbstractScreenProvider
    val logger = LogManager.Companion.logger(MinecraftServerService::class)

    init {
        fileTemplateRepository = BaseFileTemplateRepository(this.databaseConnection, this.nodeRepository, packetManager)
        serverVersionTypeRepository = CloudServerVersionTypeRepository(this.databaseConnection, null, packetManager)
        hostServiceId = runBlocking { serverRepository.connect(serviceId) }
        registerDefaults()
    }

    open fun onEnable() = runBlocking {
        try {
            val server = serverRepository.getServer<CloudServer>(serviceId) ?: throw IllegalStateException("Server not found!")
            server.state = CloudServerState.RUNNING
            serverRepository.updateServer(server)
            logger.info("Enabled cloud connector for server ${server.identifyName(false)}!")
        } catch (e: Exception) {
            logger.severe("Failed to enable cloud connector! Stopping server...", e)
            shutdown()
        }
    }

    open fun onDisable() {
        shutdown()
    }

    override fun shutdown(force: Boolean) {
        if (SHUTTINGDOWN && !force) return
        SHUTTINGDOWN = true
        runBlocking {
            serverRepository.shutdownAction.run()
        }
        super.shutdown(force)
        Thread.sleep(1500) // Wait for all threads to finish their work
    }

    protected fun registerTasks() {
        taskManager.builder()
            .task(CloudServerInfoTask(this.serviceId, this.serverRepository, this.serverPlayerProvider))
            .instant()
            .period(1500.milliseconds)
            .register()
    }

    abstract fun getConnectorPlugin(): T

    override fun configure() {
        super.configure()
        bind(ICloudFileTemplateRepository::class).toInstance(fileTemplateRepository)
        bind(ICloudServerVersionTypeRepository::class).toInstance(serverVersionTypeRepository)
        bind(ServiceId::class).annotatedWith(Names.named("hostServiceId")).toInstance(hostServiceId)
    }
}