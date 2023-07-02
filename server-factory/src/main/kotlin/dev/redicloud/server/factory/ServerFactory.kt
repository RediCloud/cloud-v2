package dev.redicloud.server.factory

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.CloudServerState
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.redisson.api.RMap
import org.redisson.api.RQueue
import java.util.*

class ServerFactory(
    databaseConnection: DatabaseConnection,
    private val nodeRepository: NodeRepository,
    private val serverRepository: ServerRepository,
    private val serverVersionRepository: CloudServerVersionRepository,
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository,
    private val fileTemplateRepository: AbstractFileTemplateRepository,
    private val javaVersionRepository: JavaVersionRepository
) {

    internal val startQueue: RMap<UUID, ServerQueueInformation> =
        databaseConnection.getClient().getMap("server-factory:queue:start")
    internal val stopQueue: RQueue<ServiceId> =
        databaseConnection.getClient().getPriorityQueue("server-factory:queue:stop")
    private val processes: MutableList<ServerProcess> = mutableListOf()
    private val logger = LogManager.logger(ServerFactory::class)

    suspend fun getStartList(): List<ServerQueueInformation> {
        return startQueue.entries.sortedWith(compareBy<MutableMap.MutableEntry<UUID, ServerQueueInformation>>
        { it.value.configurationTemplate.startPort }.thenByDescending { it.value.queueTime }).map { it.value }.toList()
    }


    fun queueStart(configurationTemplate: ConfigurationTemplate, count: Int = 1) =
        defaultScope.launch {
            val info = ServerQueueInformation(UUID.randomUUID(), configurationTemplate, queueTime = -1)
            val nodes = nodeRepository.getRegisteredNodes()
            info.calculateStartOrder(nodes)
            for (i in 0..count) {
                val clone = ServerQueueInformation(UUID.randomUUID(), configurationTemplate, info.failedStarts, info.nodeStartOrder, System.currentTimeMillis())
                startQueue[clone.uniqueId] = clone
            }
        }

    fun queueStop(serviceId: ServiceId, force: Boolean = false) =
        defaultScope.launch {
            val server = serverRepository.getServer(serviceId) ?: return@launch
            if (server.state == CloudServerState.STOPPED || server.state == CloudServerState.STOPPING && !force) return@launch
            stopQueue.add(serviceId)
        }

    //TODO: events
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
        val thisNode = nodeRepository.getNode(nodeRepository.serviceId)!!
        if (!force) {
            // check if the node is allowed to start the server
            if (configurationTemplate.nodeIds.contains(thisNode.serviceId) && configurationTemplate.nodeIds.isNotEmpty()) return NodeIsNotAllowedStartResult()

            // check if the node has enough ram
            val ramUsage = processes.sumOf { configurationTemplate.maxMemory }
            val calculatedRamUsage = ramUsage + configurationTemplate.maxMemory
            if (calculatedRamUsage > thisNode.maxMemory) return NotEnoughRamOnNodeStartResult()
            if (configurationTemplate.maxMemory > Runtime.getRuntime()
                    .freeMemory()
            ) return NotEnoughRamOnJVMStartResult()

            val servers = serverRepository.getAll()

            // Check how many servers of the template are already started and cancel if the configured globally total amount is reached
            val startAmountOfTemplate =
                servers.count { it.configurationTemplate.uniqueId == configurationTemplate.uniqueId }
            if (startAmountOfTemplate >= configurationTemplate.maxStartedServices && configurationTemplate.maxStartedServices != -1) return TooMuchServicesOfTemplateStartResult()

            // Check how many servers of the template are already started on this node and cancel if the configured node total amount is reached
            val startedAmountOfTemplateOnNode =
                servers.count { it.configurationTemplate.uniqueId == configurationTemplate.uniqueId && it.hostNodeId == thisNode.serviceId }
            if (startedAmountOfTemplateOnNode >= configurationTemplate.maxStartedServicesPerNode && configurationTemplate.maxStartedServicesPerNode != -1) return TooMuchServicesOfTemplateOnNodeStartResult()
        }

        // check if the server version is known
        if (configurationTemplate.serverVersionId == null) return UnknownServerVersionStartResult(null)
        val version = serverVersionRepository.getVersion(configurationTemplate.serverVersionId!!)
            ?: return UnknownServerVersionStartResult(null)
        if (version.typeId == null) return UnknownServerVersionStartResult(null)
        val type =
            serverVersionTypeRepository.getType(version.typeId!!) ?: return UnknownServerVersionStartResult(version)
        if (type.isUnknown()) return UnknownServerVersionStartResult(version)
        // get the version handler and update/patch the version if needed
        val versionHandler = IServerVersionHandler.getHandler(type)
        if (versionHandler.isUpdateAvailable(version)) versionHandler.update(version)
        if (versionHandler.isPatched(version) && versionHandler.isPatchVersion(version)) versionHandler.patch(version)

        // create the server process
        val serverProcess = ServerProcess(
            configurationTemplate,
            serverRepository,
            javaVersionRepository,
            serverVersionRepository,
            serverVersionTypeRepository
        )
        var cloudServer: CloudServer? = null
        try {
            // store the process
            processes.add(serverProcess)

            // get the next id for the server and create it
            val id = getForServer(configurationTemplate)
            cloudServer = serverRepository.createServer(
                CloudServer(
                    ServiceId(UUID.randomUUID(), ServiceType.SERVER),
                    configurationTemplate,
                    id,
                    thisNode.serviceId,
                    mutableListOf(),
                    false,
                    CloudServerState.PREPARING
                )
            )

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
            copier.copyVersionFiles()

            // start the server
            serverProcess.start(cloudServer)

            return SuccessStartResult(cloudServer, serverProcess)
        } catch (e: Exception) {
            // delete the server if it is created and not static
            if (cloudServer != null && !cloudServer.configurationTemplate.static) {
                serverRepository.deleteServer(cloudServer)
            }
            return UnknownErrorStartResult(e)
        }
    }

    //TODO: events
    suspend internal fun stopServer(serviceId: ServiceId, force: Boolean = true) {
        val server = serverRepository.getServer(serviceId) ?: throw IllegalArgumentException("Server not found")
        if (server.hostNodeId != nodeRepository.serviceId) throw IllegalArgumentException("Server is not on this node")
        if (server.state == CloudServerState.STOPPED || server.state == CloudServerState.STOPPING && !force) return
        val process = processes.firstOrNull { it.cloudServer!!.serviceId == serviceId }
        if (process != null) {
            process.stop()
            processes.remove(process)
        }
    }

    /**
     * Shuts down the server factory and all running processes
     */
    suspend fun shutdown() {
        processes.forEach {
            try {
                stopServer(it.cloudServer!!.serviceId)
            } catch (e: Exception) {
                try {
                    stopServer(it.cloudServer!!.serviceId, true)
                } catch (e1: Exception) {
                    logger.severe("Error while stopping server ${it.cloudServer!!.serviceId}", e1)
                }
            }
        }
        ServiceProcessHandler.PROCESS_SCOPE.cancel()
    }

    /**
     * Gets the next id for a server with the given configuration template
     */
    private suspend fun getForServer(configuration: ConfigurationTemplate): Int {
        var i = 1
        for (cloudServer in serverRepository.getAll()) {
            if (cloudServer.configurationTemplate.name != configuration.name) continue
            val id = cloudServer.configurationTemplate.name
                .replace(configuration.name, "")
                .replace(configuration.serverSplitter, "")
                .toIntOrNull() ?: continue
            if (id == cloudServer.id) i++
        }
        return i
    }

}