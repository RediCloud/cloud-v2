package dev.redicloud.server.factory

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.commands.api.CommandArgumentParser
import dev.redicloud.commands.api.AbstractCommandSuggester
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
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
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
import kotlinx.coroutines.runBlocking
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
    private val clusterConfiguration: ClusterConfiguration,
    private val configurationTemplateRepository: ConfigurationTemplateRepository
) {

    internal val startQueue: RList<ServerQueueInformation> =
        databaseConnection.getClient().getList("server-factory:queue:start")
    internal val stopQueue: RList<ServiceId> =
        databaseConnection.getClient().getList("server-factory:queue:stop")
    private val processes: MutableList<ServerProcess> = mutableListOf()
    private val logger = LogManager.logger(ServerFactory::class)

    init {
        CommandArgumentParser.PARSERS[ServerScreen::class] = ServerScreenParser(console)
        AbstractCommandSuggester.SUGGESTERS.add(ServerScreenSuggester(console))
    }

    suspend fun getStartList(): List<ServerQueueInformation> {
        return startQueue.toMutableList().sortedWith(compareBy<ServerQueueInformation>
        {
            if (it.configurationTemplate != null) {
                it.configurationTemplate.startPriority
            } else if (it.serviceId != null) {
                val configuration = runBlocking {
                    serverRepository.getServer<CloudServer>(it.serviceId)?.configurationTemplate
                }
                configuration?.startPriority ?: 50
            } else {
                50
            }
        }.thenByDescending { it.queueTime }).toList()
    }


    fun queueStart(configurationTemplate: ConfigurationTemplate, count: Int = 1) =
        defaultScope.launch {
            val info = ServerQueueInformation(UUID.randomUUID(), configurationTemplate, null, queueTime = -1)
            val nodes = nodeRepository.getRegisteredNodes()
            info.calculateStartOrder(nodes, serverRepository)
            for (i in 1..count) {
                val clone = ServerQueueInformation(UUID.randomUUID(), configurationTemplate, null, info.failedStarts, info.nodeStartOrder, System.currentTimeMillis())
                startQueue.add(clone)
            }
        }

    fun queueStart(serviceId: ServiceId) =
        defaultScope.launch {
            val info = ServerQueueInformation(UUID.randomUUID(), null, serviceId, queueTime = -1)
            val nodes = nodeRepository.getRegisteredNodes()
            info.calculateStartOrder(nodes, serverRepository)
            startQueue.add(info)
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
    suspend fun startServer(
        configurationTemplate: ConfigurationTemplate,
        force: Boolean = false
    ): StartResult {
        logger.fine("Prepare server ${configurationTemplate.uniqueId}...")

        val snapshotData = StartDataSnapshot.of(configurationTemplate)
        val dataResult = snapshotData.loadData(
            serverVersionRepository,
            serverVersionTypeRepository,
            javaVersionRepository
        )
        if (dataResult != null) return dataResult

        if (!snapshotData.versionHandler.isPatched(snapshotData.version)
            && snapshotData.versionHandler.isPatchVersion(snapshotData.version)) {
            snapshotData.versionHandler.patch(snapshotData.version)
        }

        val serviceId = ServiceId(
            UUID.randomUUID(),
            if (snapshotData.versionType.proxy) ServiceType.PROXY_SERVER else ServiceType.MINECRAFT_SERVER
        )

        // create the server process
        val serverProcess = ServerProcess(
            configurationTemplate,
            serverRepository,
            packetManager,
            bindHost,
            clusterConfiguration,
            serviceId
        )
        processes.add(serverProcess)
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

            // get the next id for the server and create it
            val id = getIdForServer(configurationTemplate)
            cloudServer = if (snapshotData.versionType.proxy) {
                serverRepository.createServer(
                    CloudProxyServer(
                        serviceId,
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
                        serviceId,
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
                serverVersionTypeRepository,
                fileTemplateRepository,
                snapshotData
            )
            serverProcess.fileCopier = copier

            // copy all templates
            copier.copyTemplates()
            // copy all version files
            copier.copyVersionFiles { serverProcess.replacePlaceholders(it) }
            // copy connector
            copier.copyConnector()

            // start the server
            return serverProcess.start(cloudServer, serverScreen, snapshotData)
        } catch (e: Exception) {
            // delete the server if it is created and not static
            try {
                stopServer(serviceId, true, true)
            }catch (_: NullPointerException) {}
            return UnknownErrorStartResult(e)
        }
    }

    suspend fun startServer(
        serviceId: ServiceId?,
        configurationTemplate: ConfigurationTemplate?,
        force: Boolean = false
    ): StartResult {
        if (serviceId == null && configurationTemplate == null) throw NullPointerException("serviceId and configurationTemplate are null")
        if (serviceId != null) {
            if (!serverRepository.existsServer<CloudServer>(serviceId)) throw NullPointerException("Static server ${serviceId.toName()} not found")
            logger.fine("Prepare static server ${serviceId.toName()}...")
            val server = serverRepository.getServer<CloudServer>(serviceId)!!
            val newConfigurationTemplate = configurationTemplateRepository.getTemplate(server.configurationTemplate.uniqueId)
            if (newConfigurationTemplate == null) return UnknownConfigurationTemplateStartResult(server.configurationTemplate.uniqueId)
            val snapshotData = StartDataSnapshot.of(newConfigurationTemplate)
            val dataResult = snapshotData.loadData(
                serverVersionRepository,
                serverVersionTypeRepository,
                javaVersionRepository
            )
            if (dataResult != null) return dataResult
            if (!snapshotData.versionHandler.isPatched(snapshotData.version)
                && snapshotData.versionHandler.isPatchVersion(snapshotData.version)) {
                snapshotData.versionHandler.patch(snapshotData.version)
            }
            // create the server process
            val serverProcess = ServerProcess(
                newConfigurationTemplate,
                serverRepository,
                packetManager,
                bindHost,
                clusterConfiguration,
                serviceId
            )
            processes.add(serverProcess)
            try {

                val thisNode = nodeRepository.getNode(nodeRepository.serviceId)!!
                if (!force) {
                    // check if the node is allowed to start the server
                    if (newConfigurationTemplate.nodeIds.contains(thisNode.serviceId) && newConfigurationTemplate.nodeIds.isNotEmpty()) return NodeIsNotAllowedStartResult()

                    // check if the node has enough ram
                    val ramUsage = processes.sumOf { newConfigurationTemplate.maxMemory }
                    val calculatedRamUsage = ramUsage + newConfigurationTemplate.maxMemory
                    if (calculatedRamUsage > thisNode.maxMemory) return NotEnoughRamOnNodeStartResult()

                    val servers = serverRepository.getRegisteredServers()

                    // Check how many servers of the template are already started and cancel if the configured globally total amount is reached
                    val startAmountOfTemplate =
                        servers.count { it.configurationTemplate.uniqueId == newConfigurationTemplate.uniqueId }
                    if (startAmountOfTemplate >= newConfigurationTemplate.maxStartedServices && newConfigurationTemplate.maxStartedServices != -1) return TooMuchServicesOfTemplateStartResult()

                    // Check how many servers of the template are already started on this node and cancel if the configured node total amount is reached
                    val startedAmountOfTemplateOnNode =
                        servers.count { it.configurationTemplate.uniqueId == newConfigurationTemplate.uniqueId && it.hostNodeId == thisNode.serviceId }
                    if (startedAmountOfTemplateOnNode >= newConfigurationTemplate.maxStartedServicesPerNode && newConfigurationTemplate.maxStartedServicesPerNode != -1) return TooMuchServicesOfTemplateOnNodeStartResult()
                }

                // get the next id for the server and create it
                val id = getIdForServer(newConfigurationTemplate)
                server.configurationTemplate = newConfigurationTemplate
                server.hostNodeId = thisNode.serviceId
                server.state = CloudServerState.PREPARING
                server.maxPlayers = newConfigurationTemplate.maxPlayers
                server.port = -1
                serverRepository.updateServer(server)

                // Create server screen
                val serverScreen = ServerScreen(server.serviceId, server.name, this.console)
                console.createScreen(serverScreen)

                // Change memory usage on node
                thisNode.currentMemoryUsage = thisNode.currentMemoryUsage + newConfigurationTemplate.maxMemory
                thisNode.hostedServers.add(server.serviceId)
                nodeRepository.updateNode(thisNode)

                // copy the files to copy server necessary files
                val copier = FileCopier(
                    serverProcess,
                    server,
                    serverVersionTypeRepository,
                    fileTemplateRepository,
                    snapshotData
                )
                serverProcess.fileCopier = copier

                // copy all templates
                copier.copyTemplates(false)
                // copy all version files
                copier.copyVersionFiles(false) { serverProcess.replacePlaceholders(it) }
                // copy connector
                copier.copyConnector()

                // start the server
                return serverProcess.start(server, serverScreen, snapshotData)
            } catch (e: Exception) {
                // delete the server if it is created and not static
                try {
                    stopServer(serviceId, true, true)
                }catch (_: NullPointerException) {}
                return UnknownErrorStartResult(e)
            }
        }else {
            return startServer(configurationTemplate!!, force)
        }
    }

    suspend internal fun stopServer(serviceId: ServiceId, force: Boolean = true, internalCall: Boolean = false) {
        val server = serverRepository.getServer<CloudServer>(serviceId) ?: throw NullPointerException("Server not found")
        if (server.hostNodeId != nodeRepository.serviceId) throw IllegalArgumentException("Server is not on this node")
        if (server.state == CloudServerState.STOPPED && !force || server.state == CloudServerState.STOPPING && !force) return
        val thisNode = nodeRepository.getNode(nodeRepository.serviceId)
        if (thisNode != null) {
            thisNode.currentMemoryUsage = thisNode.currentMemoryUsage - server.configurationTemplate.maxMemory
            if (thisNode.currentMemoryUsage < 0) thisNode.currentMemoryUsage = 0
            thisNode.hostedServers.remove(serviceId)
            nodeRepository.updateNode(thisNode)
        }
        val process = processes.firstOrNull { it.serverId == serviceId }
        if (process != null) {
            process.stop(force, internalCall)
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