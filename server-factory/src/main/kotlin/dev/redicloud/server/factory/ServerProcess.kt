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

class ServerProcess(
    val configurationTemplate: ConfigurationTemplate,
    private val serverRepository: ServerRepository
) {

    val port = findFreePort(configurationTemplate.startPort, !configurationTemplate.static)
    var process: Process? = null
    private val logger = LogManager.logger(ServerProcess::class)
    internal lateinit var fileCopier: FileCopier
    private var cloudServer: CloudServer? = null

    //TODO: events
    suspend fun start(cloudServer: CloudServer) {
        this.cloudServer = cloudServer
        val processBuilder = ProcessBuilder()
        processBuilder.environment()["REDICLOUD_SERVICE_ID"] = cloudServer.serviceId.toName()
        processBuilder.environment()["REDICLOUD_PATH"] = CLOUD_PATH
        processBuilder.environment()["REDICLOUD_LOG_LEVEL"] = getDefaultLogLevel().localizedName
        processBuilder.environment().putAll(configurationTemplate.environments)
        processBuilder.command(
            startCommand()
        )
        processBuilder.directory(fileCopier.workDirectory)
        process = processBuilder.start() //TODO: listene to process streams
        cloudServer.state = CloudServerState.STARTING
        serverRepository.updateServer(cloudServer)
        logger.fine("Started server process ${cloudServer.serviceId.toName()}")
    }

    //TODO: events
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

    private fun startCommand(): List<String> {

        val list = mutableListOf<String>(
            configurationTemplate.javaCommand,
            *configurationTemplate.jvmArguments.toTypedArray(),
            "-Xms${configurationTemplate.maxMemory}M",
            "-Xmx${configurationTemplate.maxMemory}M",
        )
        if (fileCopier.serverVersion.type.isCraftBukkitBased()) {
            list.add("-Dcom.mojang.eula.agree=true")
            list.add("-Djline.terminal=jline.UnsupportedTerminal")
        }
        list.add("-jar")
        val versionHandler = IServerVersionHandler.getHandler(fileCopier.serverVersion.type)
        val jarToExecute = versionHandler.getJar(fileCopier.serverVersion)
        list.add(jarToExecute.absolutePath)
        if (fileCopier.serverVersion.type.isCraftBukkitBased()) {
            list.add("nogui")
            list.add("-Djline.terminal=jline.UnsupportedTerminal")
        }
        list.addAll(configurationTemplate.programmArguments)
        return list
    }
}