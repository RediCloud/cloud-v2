package dev.redicloud.server.factory

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.commands.api.CommandArgumentParser
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.console.Console
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.repository.server.CloudProxyServer
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.server.factory.screens.ServerScreen
import dev.redicloud.server.factory.screens.ServerScreenParser
import dev.redicloud.server.factory.screens.ServerScreenSuggester
import dev.redicloud.service.base.utils.ClusterConfiguration
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.redisson.api.RList
import java.util.*

class ServerFactory(
    databaseConnection: DatabaseConnection,
    private val nodeRepository: NodeRepository,
    private val serverRepository: ServerRepository,
    private val serverVersionRepository: CloudServerVersionRepository,
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository,
    private val fileTemplateRepository: AbstractFileTemplateRepository,
    private val javaVersionRepository: JavaVersionRepository,
    private val packetManager: PacketManager,
    private val bindHost: String,
    private val console: Console,
    private val clusterConfiguration: ClusterConfiguration
) {

    internal val startQueue: RList<ServerQueueInformation> =
        databaseConnection.getClient().getList("server-factory:queue:start")
    internal val stopQueue: RList<ServiceId> =
        databaseConnection.getClient().getList("server-factory:queue:stop")
    private val processes: MutableList<ServerProcess> = mutableListOf()
    private val logger = LogManager.logger(ServerFactory::class)

    init {
        CommandArgumentParser.PARSERS[ServerScreen::class] = ServerScreenParser(console)
        ICommandSuggester.SUGGESTERS.add(ServerScreenSuggester(console))
    }

    suspend fun getStartList(): List<ServerQueueInformation> {
        return startQueue.toMutableList().sortedWith(compareBy<ServerQueueInformation>
        { it.configurationTemplate.startPort }.thenByDescending { it.queueTime }).toList()
    }


    fun queueStart(configurationTemplate: ConfigurationTemplate, count: Int = 1) =
        defaultScope.launch {
            val info = ServerQueueInformation(UUID.randomUUID(), configurationTemplate, queueTime = -1)
            val nodes = nodeRepository.getRegisteredNodes()
            info.calculateStartOrder(nodes)
            for (i in 1..count) {
                val clone = ServerQueueInformation(UUID.randomUUID(), configurationTemplate, info.failedStarts, info.nodeStartOrder, System.currentTimeMillis())
                startQueue.add(clone)
            }
        }

    fun queueStop(serviceId: ServiceId, force: Boolean = false) =
        defaultScope.launch {
            val server = serverRepository.getServer<CloudServer>(serviceId) ?: return@launch
            if (server.state == CloudServerState.STOPPED || server.state == CloudServerState.STOPPING && !force) return@launch
            stopQueue.add(serviceId)
        }

    /**
     * Starts a server with the given configuration template and returns the result
     * @param configurationTemplate the configuration template to use
     * @param force if the server should be started even if the configuration template does not allow it (e.g. max memory)
     * @return the result of the start
     */
    suspend internal fun startServer(
        configurationTemplate: ConfigurationTemplate,
        force: Boolean = false
    ): StartResult {
        logger.fine("Prepare server ${configurationTemplate.uniqueId}...")

        // check if the server version is known
        if (configurationTemplate.serverVersionId == null) return UnknownServerVersionStartResult(configurationTemplate.serverVersionId)
        val version = serverVersionRepository.getVersion(configurationTemplate.serverVersionId!!)
            ?: return UnknownServerVersionTypeStartResult(configurationTemplate.serverVersionId)
        if (version.typeId == null) return UnknownServerVersionTypeStartResult(null)
        val type =
            serverVersionTypeRepository.getType(version.typeId!!) ?: return UnknownServerVersionTypeStartResult(version.typeId)
        if (type.isUnknown()) return UnknownServerVersionTypeStartResult(version.typeId)

        if (version.javaVersionId == null) return UnknownJavaVersionStartResult(version.javaVersionId)
        val javaVersion = javaVersionRepository.getVersion(version.javaVersionId!!)
            ?: return UnknownJavaVersionStartResult(version.javaVersionId)

        // get the version handler and update/patch the version if needed
        val versionHandler = IServerVersionHandler.getHandler(type)
        if (!versionHandler.isPatched(version) && versionHandler.isPatchVersion(version)) versionHandler.patch(version)


        // create the server process
        val serverProcess = ServerProcess(
            configurationTemplate,
            serverRepository,
            javaVersionRepository,
            serverVersionRepository,
            serverVersionTypeRepository,
            packetManager,
            bindHost,
            clusterConfiguration
        )
        var cloudServer: CloudServer? = null
        try {

            val thisNode = nodeRepository.getNode(nodeRepository.serviceId)!!
            if (!force) {
                // check if the node is allowed to start the server
                if (configurationTemplate.nodeIds.contains(thisNode.serviceId) && configurationTemplate.nodeIds.isNotEmpty()) return NodeIsNotAllowedStartResult()

                // check if the node has enough ram
                val ramUsage = processes.sumOf { configurationTemplate.maxMemory }
                val calculatedRamUsage = ramUsage + configurationTemplate.maxMemory
                if (calculatedRamUsage > thisNode.maxMemory) return NotEnoughRamOnNodeStartResult()

                val servers = serverRepository.getRegisteredServers()

                // Check how many servers of the template are already started and cancel if the configured globally total amount is reached
                val startAmountOfTemplate =
                    servers.count { it.configurationTemplate.uniqueId == configurationTemplate.uniqueId }
                if (startAmountOfTemplate >= configurationTemplate.maxStartedServices && configurationTemplate.maxStartedServices != -1) return TooMuchServicesOfTemplateStartResult()

                // Check how many servers of the template are already started on this node and cancel if the configured node total amount is reached
                val startedAmountOfTemplateOnNode =
                    servers.count { it.configurationTemplate.uniqueId == configurationTemplate.uniqueId && it.hostNodeId == thisNode.serviceId }
                if (startedAmountOfTemplateOnNode >= configurationTemplate.maxStartedServicesPerNode && configurationTemplate.maxStartedServicesPerNode != -1) return TooMuchServicesOfTemplateOnNodeStartResult()
            }

            // store the process
            processes.add(serverProcess)

            // get the next id for the server and create it
            val id = getIdForServer(configurationTemplate)
            cloudServer = if (type.proxy) {
                serverRepository.createServer(
                    CloudProxyServer(
                        ServiceId(UUID.randomUUID(), ServiceType.PROXY_SERVER),
                        configurationTemplate,
                        id,
                        thisNode.serviceId,
                        mutableListOf(),
                        false,
                        CloudServerState.PREPARING,
                        -1,
                        configurationTemplate.maxPlayers
                    )
                )
            }else {
                serverRepository.createServer(
                    CloudMinecraftServer(
                        ServiceId(UUID.randomUUID(), ServiceType.MINECRAFT_SERVER),
                        configurationTemplate,
                        id,
                        thisNode.serviceId,
                        mutableListOf(),
                        false,
                        CloudServerState.PREPARING,
                        -1,
                        configurationTemplate.maxPlayers
                    )
                )
            }

            // Create server screen
            val serverScreen = ServerScreen(cloudServer.serviceId, cloudServer.name, this.console)
            console.createScreen(serverScreen)

            // Change memory usage on node
            thisNode.currentMemoryUsage = thisNode.currentMemoryUsage + configurationTemplate.maxMemory
            thisNode.hostedServers.add(cloudServer.serviceId)
            nodeRepository.updateNode(thisNode)

            // copy the files to copy server necessary files
            val copier = FileCopier(
                serverProcess,
                cloudServer,
                serverVersionRepository,
                serverVersionTypeRepository,
                fileTemplateRepository
            )
            serverProcess.fileCopier = copier

            // copy all templates
            copier.copyTemplates()
            // copy all version files
            copier.copyVersionFiles { serverProcess.replacePlaceholders(it) }
            // copy connector
            copier.copyConnector()

            // start the server
            return serverProcess.start(cloudServer, serverScreen)
        } catch (e: Exception) {
            // delete the server if it is created and not static
            if (cloudServer != null) {
                stopServer(cloudServer.serviceId, true)
            }
            return UnknownErrorStartResult(e)
        }
    }

    suspend internal fun stopServer(serviceId: ServiceId, force: Boolean = true) {
        val server = serverRepository.getServer<CloudServer>(serviceId) ?: throw NullPointerException("Server not found")
        if (server.hostNodeId != nodeRepository.serviceId) throw IllegalArgumentException("Server is not on this node")
        if (server.state == CloudServerState.STOPPED || server.state == CloudServerState.STOPPING && !force) return
        val thisNode = nodeRepository.getNode(nodeRepository.serviceId)
        if (thisNode != null) {
            thisNode.currentMemoryUsage = thisNode.currentMemoryUsage - server.configurationTemplate.maxMemory
            if (thisNode.currentMemoryUsage < 0) thisNode.currentMemoryUsage = 0
            thisNode.hostedServers.remove(serviceId)
            nodeRepository.updateNode(thisNode)
        }
        val process = processes.firstOrNull { it.cloudServer?.serviceId == serviceId }
        if (process != null) {
            process.stop()
            processes.remove(process)
        }
    }

    /**
     * Shuts down the server factory and all running processes
     */
    suspend fun shutdown() {
        processes.toList().forEach {
            try {
                stopServer(it.cloudServer!!.serviceId)
            } catch (e: Exception) {
                try {
                    stopServer(it.cloudServer!!.serviceId, true)
                } catch (e1: Exception) {
                    if (e1 is NullPointerException) return@forEach
                    logger.severe("Error while stopping server ${it.cloudServer!!.serviceId.toName()}", e1)
                }
            }
        }
        ServerProcessHandler.PROCESS_SCOPE.cancel()
    }

    /**
     * Gets the next id for a server with the given configuration template
     */
    private suspend fun getIdForServer(configuration: ConfigurationTemplate): Int {
        val usedIds = serverRepository.getRegisteredServers()
            .filter { it.configurationTemplate.name == configuration.name }
            .map { it.id }
        var i = 1
        while (usedIds.contains(i)) {
            i++
        }
        return i
    }

}