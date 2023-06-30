package dev.redicloud.server.factory

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.CloudServerState
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.file.FileTemplateRepository
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.cancel
import org.redisson.api.RQueue
import java.util.*

class ServerFactory(
    databaseConnection: DatabaseConnection,
    private val nodeRepository: NodeRepository,
    private val serverRepository: ServerRepository,
    private val serverVersionRepository: ServerVersionRepository,
    private val fileTemplateRepository: FileTemplateRepository
) {

    //TODO: move to server queue task
    private val queue: RQueue<ConfigurationTemplate> =
        databaseConnection.getClient().getPriorityQueue("server-factory-queue")
    private val processes: MutableList<ServerProcess> = mutableListOf()
    private val logger = LogManager.logger(ServerFactory::class)

    //TODO: events
    /**
     * Starts a server with the given configuration template and returns the result
     * @param configurationTemplate the configuration template to use
     * @param force if the server should be started even if the configuration template does not allow it (e.g. max memory)
     * @return the result of the start
     */
    suspend fun startServer(configurationTemplate: ConfigurationTemplate, force: Boolean = false): StartResult {
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
        val version = serverVersionRepository.getVersion(configurationTemplate.serverVersionId)
        if (version == null || version.type.isUnknown()) return UnknownServerVersionStartResult(version)
        // get the version handler and update/patch the version if needed
        val versionHandler = IServerVersionHandler.getHandler(version.type)
        if (versionHandler.isUpdateAvailable(version)) versionHandler.update(version)
        if (versionHandler.isPatched(version) && versionHandler.isPatchVersion(version)) versionHandler.patch(version)

        // create the server process
        val serverProcess = ServerProcess(configurationTemplate, serverRepository)
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
                    CloudServerState.PREPARING
                )
            )

            // copy the files to copy server necessary files
            val copier = FileCopier(serverProcess, cloudServer, serverVersionRepository, fileTemplateRepository)
            serverProcess.fileCopier = copier

            // copy all templates
            copier.copyTemplates()
            // copy all version files
            copier.copyVersionFiles()

            // start the server
            serverProcess.start(cloudServer)

            return SuccessStartResult(cloudServer, serverProcess)
        }catch (e: Exception) {
            // delete the server if it is created and not static
            if (cloudServer != null && !cloudServer.configurationTemplate.static) {
                serverRepository.deleteServer(cloudServer)
            }
            return UnknownErrorStartResult(e)
        }
    }

    //TODO: events
    suspend fun stop(serviceId: ServiceId, force: Boolean = true) {
        //TODO: stop service
    }

    /**
     * Shuts down the server factory and all running processes
     */
    suspend fun shutdown() {
        processes.forEach {
            stop(it.cloudServer!!.serviceId)
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