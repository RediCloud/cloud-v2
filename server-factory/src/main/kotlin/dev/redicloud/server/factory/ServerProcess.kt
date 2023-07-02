package dev.redicloud.server.factory

import dev.redicloud.logging.LogManager
import dev.redicloud.logging.getDefaultLogLevel
import dev.redicloud.repository.java.version.JavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.CloudServerState
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.utils.CLOUD_PATH
import dev.redicloud.utils.findFreePort
import kotlinx.coroutines.runBlocking

class ServerProcess(
    val configurationTemplate: ConfigurationTemplate,
    private val serverRepository: ServerRepository,
    private val javaVersionRepository: JavaVersionRepository,
    private val serverVersionRepository: CloudServerVersionRepository,
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository
) {

    val port = findFreePort(configurationTemplate.startPort, !configurationTemplate.static)
    var process: Process? = null
    var handler: ServiceProcessHandler? = null
    private val logger = LogManager.logger(ServerProcess::class)
    internal lateinit var fileCopier: FileCopier
    internal var cloudServer: CloudServer? = null

    //TODO: events
    /**
     * Starts the server process
     * @param cloudServer the cloud server instance
     */
    suspend fun start(cloudServer: CloudServer) {
        this.cloudServer = cloudServer
        val processBuilder = ProcessBuilder()
        // set environment variables
        processBuilder.environment()["REDICLOUD_SERVICE_ID"] = cloudServer.serviceId.toName()
        processBuilder.environment()["REDICLOUD_PATH"] = CLOUD_PATH
        processBuilder.environment()["REDICLOUD_PORT"] = port.toString()
        processBuilder.environment()["REDICLOUD_LOG_LEVEL"] = getDefaultLogLevel().localizedName
        processBuilder.environment().putAll(configurationTemplate.environments)

        if (configurationTemplate.javaVersionId == null) throw IllegalStateException("Java version is not set that is required of ${configurationTemplate.name} configuration")
        if (configurationTemplate.serverVersionId == null) throw IllegalStateException("Server version is not set that is required of ${configurationTemplate.name} configuration")

        val javaVersion = javaVersionRepository.getVersion(configurationTemplate.javaVersionId!!)
            ?: throw IllegalStateException("Java version ${configurationTemplate.javaVersionId} not found that is required of ${configurationTemplate.name} configuration")
        val serverVersion = serverVersionRepository.getVersion(configurationTemplate.serverVersionId!!)
            ?: throw IllegalStateException("Server version ${configurationTemplate.serverVersionId} not found that is required of ${configurationTemplate.name} configuration")

        if (serverVersion.typeId == null)
            throw IllegalStateException("Server version type of version ${serverVersion.getDisplayName()} is not set that is required of ${configurationTemplate.name} configuration")

        val versionType = serverVersionTypeRepository.getType(serverVersion.typeId!!) ?: throw IllegalStateException("Server version type ${serverVersion.typeId} not found")

        // set command
        processBuilder.command(
            startCommand(versionType, javaVersion)
        )
        // set working directory
        processBuilder.directory(fileCopier.workDirectory)
        process = processBuilder.start()
        // create handler and listen for exit
        handler = ServiceProcessHandler(process!!, cloudServer)
        handler!!.onExit { runBlocking { stop(false) } }

        cloudServer.state = CloudServerState.STARTING
        serverRepository.updateServer(cloudServer)

        logger.fine("Started server process ${cloudServer.serviceId.toName()}")
    }

    //TODO: events
    /**
     * Stops the server process
     */
    suspend fun stop(force: Boolean = false) {
        logger.fine("Stopped server process ${configurationTemplate.uniqueId}")

        if (cloudServer != null) {
            cloudServer!!.state = CloudServerState.STOPPING
            serverRepository.updateServer(cloudServer!!)
        }

        if (process != null) {
            if (force) {
                process!!.destroyForcibly()
            } else {
                process!!.destroy()
            }
        }

        if (cloudServer != null) {
            cloudServer!!.state = CloudServerState.STOPPED
            serverRepository.updateServer(cloudServer!!)
        }

        logger.fine("Stopped server process ${configurationTemplate.uniqueId}")

        if (cloudServer?.unregisterAfterDisconnect() == true) {
            serverRepository.deleteServer(cloudServer!!)
        }
    }

    /**
     * Creates the command to start the server with based server version type configurations
     * provide also placeholders like %PORT% or %SERVICE_ID%
     */
    private fun startCommand(type: CloudServerVersionType, javaVersion: JavaVersion): List<String> {
        if(!javaVersion.isLocated(serverRepository.serviceId)) {
            javaVersion.located[serverRepository.serviceId.id] = javaVersion.autoLocate()?.absolutePath ?: throw IllegalStateException("Java version ${javaVersion.id} not found")
        }
        val javaPath = javaVersion.located[serverRepository.serviceId.id]
        if (javaPath == null || javaPath.isEmpty()) throw IllegalStateException("Java version ${javaVersion.id} not found")

        val list = mutableListOf<String>(
            javaPath,
            *configurationTemplate.jvmArguments.toTypedArray(),
            "-Xms${configurationTemplate.maxMemory}M",
            "-Xmx${configurationTemplate.maxMemory}M",
        )
        type.jvmArguments.forEach { list.add(replacePlaceholders(it)) }
        list.add("-jar")
        val versionHandler = IServerVersionHandler.getHandler(type)
        val jarToExecute = versionHandler.getJar(fileCopier.serverVersion)
        list.add(jarToExecute.absolutePath)
        type.programmArguments.forEach { list.add(replacePlaceholders(it)) }
        list.addAll(configurationTemplate.programmArguments)
        return list
    }

    private fun replacePlaceholders(text: String): String =
        text.replace("%PORT%", port.toString())
            .replace("%SERVICE_ID%", cloudServer?.serviceId?.toName() ?: "unknown")
            .replace("%SERVICE_NAME%", cloudServer?.serviceId?.toName() ?: "unknown")
            .replace("%HOSTNAME%", cloudServer?.currentOrLastsession()?.ipAddress ?: "127.0.0.1")

}