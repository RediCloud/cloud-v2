package dev.redicloud.server.factory

import dev.redicloud.logging.LogManager
import dev.redicloud.logging.getDefaultLogLevel
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.CloudServerState
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.server.version.utils.CloudServerVersionType
import dev.redicloud.repository.temlate.configuration.ConfigurationTemplate
import dev.redicloud.utils.CLOUD_PATH
import dev.redicloud.utils.findFreePort
import kotlinx.coroutines.runBlocking

class ServerProcess(
    val configurationTemplate: ConfigurationTemplate,
    private val serverRepository: ServerRepository
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
        // set command
        processBuilder.command(
            startCommand()
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
    suspend fun stop(force: Boolean) {
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
    private fun startCommand(): List<String> {

        val list = mutableListOf<String>(
            configurationTemplate.javaCommand,
            *configurationTemplate.jvmArguments.toTypedArray(),
            "-Xms${configurationTemplate.maxMemory}M",
            "-Xmx${configurationTemplate.maxMemory}M",
        )
        fileCopier.serverVersion.type.jvmArguments.forEach { list.add(replacePlaceholders(it)) }
        list.add("-jar")
        val versionHandler = IServerVersionHandler.getHandler(fileCopier.serverVersion.type)
        val jarToExecute = versionHandler.getJar(fileCopier.serverVersion)
        list.add(jarToExecute.absolutePath)
        fileCopier.serverVersion.type.programmArguments.forEach { list.add(replacePlaceholders(it)) }
        list.addAll(configurationTemplate.programmArguments)
        return list
    }

    private fun replacePlaceholders(text: String): String =
        text.replace("%PORT%", port.toString())
            .replace("%SERVICE_ID%", cloudServer?.serviceId?.toName() ?: "unknown")
            .replace("%SERVICE_NAME%", cloudServer?.serviceId?.toName() ?: "unknown")
            .replace("%HOSTNAME%", cloudServer?.currentOrLastsession()?.ipAddress ?: "127.0.0.1")

}