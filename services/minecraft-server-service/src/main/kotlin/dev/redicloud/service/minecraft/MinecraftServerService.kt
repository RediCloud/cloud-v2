package dev.redicloud.service.minecraft

import com.google.inject.name.Names
import dev.redicloud.api.IConnectorAPI
import dev.redicloud.api.provider.IServerPlayerProvider
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.base.repository.BaseFileTemplateRepository
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import dev.redicloud.service.minecraft.repositories.connect
import dev.redicloud.service.minecraft.tasks.CloudServerInfoTask
import dev.redicloud.api.utils.DATABASE_JSON
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.factory.ICloudRemoteServerFactory
import dev.redicloud.api.utils.ICurrentServerData
import dev.redicloud.api.version.ICloudServerVersion
import dev.redicloud.api.version.ICloudServerVersionType
import dev.redicloud.modules.ModuleHandler
import dev.redicloud.server.factory.RemoteServerFactory
import dev.redicloud.service.minecraft.utils.CurrentServerData
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

abstract class MinecraftServerService<T> : BaseService(
    DatabaseConfiguration.fromFile(DATABASE_JSON.getFile()),
    null,
    ServiceId.fromString(System.getenv("RC_SERVICE_ID"))
), IConnectorAPI {

    companion object {
        private val logger = LogManager.logger(MinecraftServerService::class)
    }

    override val fileTemplateRepository: AbstractFileTemplateRepository
            = BaseFileTemplateRepository(this.databaseConnection, this.nodeRepository, packetManager)
    override val serverVersionTypeRepository: CloudServerVersionTypeRepository
            = CloudServerVersionTypeRepository(this.databaseConnection, null, packetManager)
    val currentServerData: CurrentServerData = runBlocking {
        CurrentServerData(
            getServer().serviceId,
            getServer().name,
            getServer().id,
            getServer().maxPlayers,
            getServer().connectedPlayers,
            getServer().state,
            getServer().configurationTemplate.name,
            getVersion().displayName
        )
    }
    private val hostServiceId: ServiceId = runBlocking { serverRepository.connect(serviceId) }
    override val moduleHandler: ModuleHandler
        = ModuleHandler(serviceId, loadModuleRepositoryUrls(), eventManager, packetManager, runBlocking { getVersionType() }, databaseConnection)
    abstract val screenProvider: AbstractScreenProvider
    val remoteServerFactory: RemoteServerFactory
        = RemoteServerFactory(this.databaseConnection, this.nodeRepository, this.serverRepository)

    init {
        packetManager.registerCategoryChannel(currentServerData.configurationTemplateName)
        registerDefaults()
    }

    private suspend fun getVersionType(): ICloudServerVersionType {
        return serverVersionTypeRepository.getType(getVersion().typeId!!)!!
    }

    private suspend fun getVersion(): ICloudServerVersion {
        return serverVersionRepository.getVersion(getServer().configurationTemplate.serverVersionId!!)!!
    }

    private suspend fun getServer(): CloudServer {
        return serverRepository.getServer(serviceId) ?: throw IllegalStateException("Server not found!")
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
            .task(CloudServerInfoTask(this.serviceId, this.serverRepository, this.playerProvider, this.currentServerData))
            .instant()
            .period(1500.milliseconds)
            .register()
    }

    abstract fun getConnectorPlugin(): T

    override fun configure() {
        super.configure()
        bind(ServiceId::class).annotatedWith(Names.named("host")).toInstance(hostServiceId)
        bind(IConnectorAPI::class.java).toInstance(this)
        bind(ICurrentServerData::class.java).toInstance(currentServerData)
        bind(ICloudRemoteServerFactory::class.java).toInstance(remoteServerFactory)
    }
}