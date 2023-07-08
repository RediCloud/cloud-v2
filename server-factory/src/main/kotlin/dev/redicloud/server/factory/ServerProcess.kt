package dev.redicloud.server.factory

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.service.base.packets.CloudServiceShutdownPacket
import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.logging.getDefaultLogLevel
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.java.version.JavaVersion
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.server.factory.screens.ServerScreen
import dev.redicloud.service.base.utils.ClusterConfiguration
import dev.redicloud.utils.CLOUD_PATH
import dev.redicloud.utils.LIB_FOLDER
import dev.redicloud.utils.ProcessConfiguration
import dev.redicloud.utils.findFreePort
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class ServerProcess(
    val configurationTemplate: ConfigurationTemplate,
    private val serverRepository: ServerRepository,
    private val packetManager: PacketManager,
    private val bindHost: String,
    private val clusterConfiguration: ClusterConfiguration,
    val serverId: ServiceId
) {

    val port = findFreePort(configurationTemplate.startPort, !configurationTemplate.static)
    var process: Process? = null
    var handler: ServerProcessHandler? = null
    var processConfiguration: ProcessConfiguration? = null
    private val logger = LogManager.logger(ServerProcess::class)
    internal lateinit var fileCopier: FileCopier
    internal var cloudServer: CloudServer? = null
    internal var stopped = false

    companion object {
        val SERVER_STOP_TIMEOUT = System.getProperty("redicloud.server.stop.timeout", "20").toInt()
    }

    /**
     * Starts the server process
     * @param cloudServer the cloud server instance
     */
    suspend fun start(cloudServer: CloudServer, serverScreen: ServerScreen, snapshotData: StartDataSnapshot): StartResult {
        if (stopped) return StoppedStartResult()
        this.cloudServer = cloudServer
        processConfiguration = ProcessConfiguration.collect(
            configurationTemplate,
            snapshotData.version,
            snapshotData.versionType
        )
        cloudServer.port = port
        val processBuilder = ProcessBuilder()
        // set environment variables
        processBuilder.environment()["RC_SERVICE_ID"] = cloudServer.serviceId.toName()
        processBuilder.environment()["RC_PATH"] = CLOUD_PATH
        processBuilder.environment()["RC_HOST"] = bindHost
        processBuilder.environment()["RC_PORT"] = port.toString()
        processBuilder.environment()["RC_LOG_LEVEL"] = getDefaultLogLevel().localizedName
        processBuilder.environment()["LIBRARY_FOLDER"] = LIB_FOLDER.getFile().absolutePath
        processBuilder.environment().putAll(processConfiguration!!.environmentVariables)

        val javaPath = snapshotData.javaVersion.located[serverRepository.serviceId.id]
        if (javaPath.isNullOrEmpty()) return JavaVersionNotInstalledStartResult(snapshotData.javaVersion)

        // set command
        processBuilder.command(
            startCommand(snapshotData.versionType, javaPath, snapshotData)
        )
        // set working directory
        processBuilder.directory(fileCopier.workDirectory)

        if (stopped) return StoppedStartResult()

        process = processBuilder.start()
        // create handler and listen for exit
        handler = ServerProcessHandler(process!!, cloudServer, serverScreen)
        handler!!.onExit { runBlocking { stop(internalCall =  true) } }

        cloudServer.state = CloudServerState.STARTING
        cloudServer.port = port
        serverRepository.updateServer(cloudServer)

        logger.fine("Started server process ${cloudServer.serviceId.toName()}")
        return SuccessStartResult(cloudServer, this)
    }

    /**
     * Stops the server process
     */
    suspend fun stop(force: Boolean = false, internalCall: Boolean = false) {
        if (stopped) return
        stopped = true
        if (!serverRepository.existsServer<CloudServer>(serverId)) return
        cloudServer = serverRepository.getServer(serverId) ?: return
        val identifier = cloudServer?.serviceId?.toName() ?: configurationTemplate.uniqueId
        if (internalCall) {
            logger.fine("Detected process exit of $identifier")
            if (cloudServer?.connected == true) {
                logger.warning("§cServer ${toConsoleValue(cloudServer!!.name, false)} stopped unexpectedly!")
            }
        }else {
            logger.fine("Stopped server process $identifier")
        }

        if (cloudServer != null && !internalCall) {
            cloudServer!!.state = CloudServerState.STOPPING
            serverRepository.updateServer(cloudServer!!)
            val response = packetManager.publish(CloudServiceShutdownPacket(), cloudServer!!.serviceId)
            val answer = response.withTimeOut(4.seconds).waitBlocking()
            if (answer != null) {
                var seconds = 0
                while (cloudServer!!.connected && seconds < SERVER_STOP_TIMEOUT) {
                    Thread.sleep(1000)
                    seconds++
                    cloudServer = serverRepository.getServer(serverId)!!
                }
                if (cloudServer!!.connected) {
                    logger.warning("§cServer ${toConsoleValue(cloudServer!!.name, false)} stop request timed out. Stopping process manually!")
                }
            } else {
                logger.warning("§cServer ${toConsoleValue(cloudServer!!.name, false)} does not respond to stop request. Stopping process manually!")
            }
        }

        if (process != null && process!!.isAlive) {
            if (force) {
                process!!.destroyForcibly()
            } else {
                process!!.destroy()
            }
        }

        fileCopier.workDirectory.deleteRecursively()

        if (cloudServer != null) {
            cloudServer = serverRepository.getServer(serverId)!!
            cloudServer!!.state = CloudServerState.STOPPED
            cloudServer!!.connected = false
            cloudServer!!.connectedPlayers.clear()
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
    private fun startCommand(type: CloudServerVersionType, javaPath: String, snapshotData: StartDataSnapshot): List<String> {
        if (!snapshotData.javaVersion.isLocated(serverRepository.serviceId)) {
            snapshotData.javaVersion.located[serverRepository.serviceId.id] = snapshotData.javaVersion.autoLocate()?.absolutePath
                ?: throw IllegalStateException("Java version ${snapshotData.javaVersion.id} not found")
        }

        val list = mutableListOf<String>(
            javaPath,
        )

        if ((snapshotData.javaVersion.info?.major ?: -1) > 8) {
            list.apply {
                add("--add-opens=java.base/java.lang=ALL-UNNAMED")
                add("--add-opens=java.base/java.util.concurrent=ALL-UNNAMED")
                add("--add-opens=java.base/java.text=ALL-UNNAMED")
                add("--add-opens=java.base/java.util=ALL-UNNAMED")
                add("--add-opens=java.base/java.math=ALL-UNNAMED")
                add("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
                add("--add-opens=java.base/java.net=ALL-UNNAMED")
                add("--add-opens=java.base/sun.net.www.protocol.https=ALL-UNNAMED")
            }
        }
        configurationTemplate.jvmArguments.forEach { list.add(replacePlaceholders(it)) }
        list.add("-Xms${configurationTemplate.maxMemory}M")
        list.add("-Xmx${configurationTemplate.maxMemory}M")

        type.jvmArguments.forEach { list.add(replacePlaceholders(it)) }
        list.add("-jar")
        val versionHandler = IServerVersionHandler.getHandler(type)
        val jarToExecute = versionHandler.getJar(snapshotData.version)
        list.add(jarToExecute.absolutePath)
        processConfiguration!!.programmParameters.forEach { list.add(replacePlaceholders(it)) }
        list.addAll(processConfiguration!!.programmParameters)
        return list
    }

    fun replacePlaceholders(text: String): String =
        text.replace("%PORT%", port.toString())
            .replace("%SERVICE_ID%", cloudServer?.serviceId?.toName() ?: "unknown")
            .replace("%SERVICE_NAME%", cloudServer?.serviceId?.toName() ?: "unknown")
            .replace("%HOSTNAME%", cloudServer?.currentOrLastsession()?.ipAddress ?: "127.0.0.1")
            .replace("%PROXY_SECRET%", clusterConfiguration.get("proxy-secret")!!)

}