package dev.redicloud.service.base

import dev.redicloud.commands.api.CommandArgumentParser
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.java.version.JavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.repository.template.file.FileTemplateRepository
import dev.redicloud.service.base.packets.ServicePingPacket
import dev.redicloud.service.base.packets.ServicePingResponse
import dev.redicloud.service.base.parser.*
import dev.redicloud.service.base.suggester.*
import dev.redicloud.tasks.CloudTaskManager
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

abstract class BaseService(
    databaseConfiguration: DatabaseConfiguration,
    _databaseConnection: DatabaseConnection?,
    val serviceId: ServiceId
) {

    companion object {
        val LOGGER = LogManager.logger(BaseService::class)
        var SHUTTINGDOWN = false
    }

    val databaseConnection: DatabaseConnection

    val nodeRepository: NodeRepository
    val serverRepository: ServerRepository
    val serverVersionRepository: ServerVersionRepository
    val fileTemplateRepository: FileTemplateRepository
    val configurationTemplateRepository: ConfigurationTemplateRepository
    val serverVersionTypeRepository: CloudServerVersionTypeRepository
    val javaVersionRepository: JavaVersionRepository

    val packetManager: PacketManager
    val eventManager: EventManager
    val taskManager: CloudTaskManager

    init {
        runBlocking {
            ServerVersion.loadIfNotLoaded()
        }
        databaseConnection = if (_databaseConnection != null && _databaseConnection.isConnected()) {
            _databaseConnection
        } else {
            DatabaseConnection(databaseConfiguration, serviceId, GsonCodec())
        }
        try {
            if (!databaseConnection.isConnected()) databaseConnection.connect()
        } catch (e: Exception) {
            LOGGER.severe("Failed to connect to database", e)
            exitProcess(-1)
        }

        packetManager = PacketManager(databaseConnection, serviceId)
        eventManager = EventManager("base-event-manager", packetManager)
        taskManager = CloudTaskManager(eventManager, packetManager)

        javaVersionRepository = JavaVersionRepository(serviceId, databaseConnection)
        nodeRepository = NodeRepository(databaseConnection, serviceId, packetManager)
        serverVersionTypeRepository = CloudServerVersionTypeRepository(databaseConnection)
        serverVersionRepository = ServerVersionRepository(databaseConnection)
        serverRepository = ServerRepository(databaseConnection, serviceId, packetManager)
        fileTemplateRepository = FileTemplateRepository(databaseConnection, nodeRepository)
        configurationTemplateRepository = ConfigurationTemplateRepository(databaseConnection)

        this.registerParsers()
        this.registerSuggesters()
        this.registerPackets()
    }

    open fun shutdown() {
        SHUTTINGDOWN = true
        nodeRepository.shutdownAction.run()
        serverRepository.shutdownAction.run()
        taskManager.getTasks().forEach { it.cancel() }
        packetManager.disconnect()
        databaseConnection.disconnect()
    }

    private fun registerParsers() {
        CommandArgumentParser.PARSERS[CloudNode::class] = CloudNodeParser(this.nodeRepository)
        CommandArgumentParser.PARSERS[CloudServer::class] = CloudServerParser(this.serverRepository)
        CommandArgumentParser.PARSERS[CloudServerVersion::class] = CloudServerVersionParser(this.serverVersionRepository)
        CommandArgumentParser.PARSERS[CloudServerVersionType::class] = CloudServerVersionTypeParser(this.serverVersionTypeRepository)
        CommandArgumentParser.PARSERS[JavaVersion::class] = JavaVersionParser(this.javaVersionRepository)
        CommandArgumentParser.PARSERS[ServerVersion::class] = ServerVersionParser()
    }

    private fun registerSuggesters() {
        ICommandSuggester.SUGGESTERS.add(RegisteredCloudNodeSuggester(this.nodeRepository))
        ICommandSuggester.SUGGESTERS.add(ConnectedCloudNodeSuggester(this.nodeRepository))
        ICommandSuggester.SUGGESTERS.add(CloudServerVersionSuggester(this.serverVersionRepository))
        ICommandSuggester.SUGGESTERS.add(CloudServerVersionTypeSuggester(this.serverVersionTypeRepository))
        ICommandSuggester.SUGGESTERS.add(JavaVersionSuggester(this.javaVersionRepository))
        ICommandSuggester.SUGGESTERS.add(ServerVersionSuggester())
    }

    private fun registerPackets() {
        packetManager.registerPacket(ServicePingPacket::class)
        packetManager.registerPacket(ServicePingResponse::class)
    }

}