package dev.redicloud.server.factory

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.CloudServerState
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.repository.temlate.configuration.ConfigurationTemplate
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
    suspend fun startServer(configurationTemplate: ConfigurationTemplate, force: Boolean = false): StartResult {
        logger.fine("Prepare server ${configurationTemplate.uniqueId}...")
        val thisNode = nodeRepository.getNode(nodeRepository.serviceId)!!
        if (!force) {
            if (configurationTemplate.nodeIds.contains(thisNode.serviceId) && configurationTemplate.nodeIds.isNotEmpty()) return NodeIsNotAllowedStartResult()

            val ramUsage = processes.sumOf { configurationTemplate.maxMemory }
            val calculatedRamUsage = ramUsage + configurationTemplate.maxMemory
            if (calculatedRamUsage > thisNode.maxMemory) return NotEnoughRamOnNodeStartResult()
            if (configurationTemplate.maxMemory > Runtime.getRuntime()
                    .freeMemory()
            ) return NotEnoughRamOnJVMStartResult()

            val servers = serverRepository.getAll()

            val startAmountOfTemplate =
                servers.count { it.configurationTemplate.uniqueId == configurationTemplate.uniqueId }
            if (startAmountOfTemplate >= configurationTemplate.maxStartedServices && configurationTemplate.maxStartedServices != -1) return TooMuchServicesOfTemplateStartResult()

            val startedAmountOfTemplateOnNode =
                servers.count { it.configurationTemplate.uniqueId == configurationTemplate.uniqueId && it.hostNodeId == thisNode.serviceId }
            if (startedAmountOfTemplateOnNode >= configurationTemplate.maxStartedServicesPerNode && configurationTemplate.maxStartedServicesPerNode != -1) return TooMuchServicesOfTemplateOnNodeStartResult()
        }

        val serverProcess = ServerProcess(configurationTemplate, serverRepository)
        var cloudServer: CloudServer? = null
        try {
            processes.add(serverProcess)

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

            val copier = FileCopier(serverProcess, cloudServer, serverVersionRepository, fileTemplateRepository)
            serverProcess.fileCopier = copier

            copier.copyTemplates()
            copier.copyVersionFiles()

            serverProcess.start(cloudServer)

            return SuccessStartResult(cloudServer, serverProcess)
        }catch (e: Exception) {
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

    suspend fun shutdown() {
        processes.forEach {
            stop(it.cloudServer!!.serviceId)
        }
        ServiceProcessHandler.PROCESS_SCOPE.cancel()
    }

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