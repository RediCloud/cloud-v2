package dev.redicloud.server.factory

import dev.redicloud.api.events.internal.server.CloudServerDisconnectedEvent
import dev.redicloud.api.server.factory.ICloudServerProcess
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplate
import dev.redicloud.api.utils.CLOUD_PATH
import dev.redicloud.api.utils.LIB_FOLDER
import dev.redicloud.api.utils.ProcessConfiguration
import dev.redicloud.api.utils.ProcessHandler
import dev.redicloud.api.version.IServerVersionHandler
import dev.redicloud.console.utils.ScreenProcessHandler
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.logging.getDefaultLogLevel
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.server.factory.screens.ServerScreen
import dev.redicloud.server.factory.utils.*
import dev.redicloud.service.base.packets.service.CloudServiceShutdownPacket
import dev.redicloud.service.base.utils.ClusterConfiguration
import dev.redicloud.utils.blockPort
import dev.redicloud.utils.findFreePort
import dev.redicloud.utils.freePort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class ServerProcess(
    override val configurationTemplate: ICloudConfigurationTemplate,
    private val serverRepository: ServerRepository,
    private val packetManager: PacketManager,
    private val eventManager: EventManager,
    private val bindHost: String,
    private val clusterConfiguration: ClusterConfiguration,
    override val serverId: ServiceId,
    override val hostServiceId: ServiceId,
    private val scope: CoroutineScope
) : ICloudServerProcess {

    override val port: Int
    override var process: Process? = null
    override var processHandler: ProcessHandler? = null
    override var processConfiguration: ProcessConfiguration? = null
    internal lateinit var fileCopier: FileCopier
    internal var cloudServer: CloudServer? = null
    internal var stopped = false

    companion object {
        private val logger = LogManager.logger(ServerProcess::class)
        private const val DEFAULT_START_PORT = 40000
        private val STOP_POLL_INTERVAL = 1.seconds
        private const val JAVA_8_MAJOR_VERSION = 8

        /**
         * JPMS module opens/exports required by dependencies on Java 9+.
         * Keep in sync with `scripts/start/start.sh`.
         */
        private val JPMS_OPENS = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED", // Redisson, Gson, Guice, Kotlin Reflect
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED", // Guice, Kotlin Reflect
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED", // Redisson
            "--add-opens=java.base/java.text=ALL-UNNAMED", // Gson date/time
            "--add-opens=java.base/java.util=ALL-UNNAMED", // Redisson, Gson
            "--add-opens=java.base/java.math=ALL-UNNAMED", // Redisson, Gson
            "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED", // Netty Unsafe
            "--add-opens=java.base/java.nio=ALL-UNNAMED", // Netty DirectByteBuffer
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", // Netty NIO selector
            "--add-opens=java.base/java.net=ALL-UNNAMED", // Ktor CIO, ClassLoader
            "--add-opens=java.base/sun.net.www.protocol.https=ALL-UNNAMED" // Ktor HTTPS
        )
        val SERVER_STOP_TIMEOUT = System.getProperty("redicloud.server.stop.timeout", "20").toInt()
    }

    init {
        port = if (configurationTemplate.startPort == -1) {
            findFreePort(DEFAULT_START_PORT, true)
        } else {
            findFreePort(configurationTemplate.startPort, false)
        }
        blockPort(port)
    }

    /**
     * Starts the server process
     * @param cloudServer the cloud server instance
     */
    suspend fun start(
        cloudServer: CloudServer,
        serverScreen: ServerScreen,
        snapshotData: StartDataSnapshot
    ): StartResult {
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
        processBuilder.environment()["PROXY_SECRET"] = clusterConfiguration.get("proxy-secret") ?: "redicloud_secret"
        processBuilder.environment().putAll(processConfiguration!!.environmentVariables)

        val javaPath = snapshotData.javaVersion.located[hostServiceId.id]
        if (javaPath.isNullOrEmpty()) return JavaVersionNotInstalledStartResult(snapshotData.javaVersion)

        // set command
        processBuilder.command(
            startCommand(snapshotData.versionType, javaPath, snapshotData)
        )
        // set working directory
        processBuilder.directory(fileCopier.workDirectory)

        if (stopped) return StoppedStartResult()

        process = withContext(Dispatchers.IO) {
            processBuilder.start()
        }
        // create handler and listen for exit
        processHandler = ScreenProcessHandler(process!!, serverScreen)
        processHandler!!.onExit { scope.launch { stop(internalCall = true) } }

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
        freePort(port)
        stopped = true
        if (!serverRepository.existsServer<CloudServer>(serverId)) return
        cloudServer = serverRepository.getServer(serverId) ?: return

        val unexpectedlyStop = if (internalCall) {
            handleInternalStop()
        } else {
            logger.fine("Stopped server process ${cloudServer?.serviceId?.toName() ?: configurationTemplate.uniqueId}")
            false
        }

        if (cloudServer != null && !internalCall) {
            sendStopRequestAndWait()
        }

        destroyProcess(force)
        cleanupWorkDirectory(unexpectedlyStop)
        logger.fine("Stopped server process ${configurationTemplate.uniqueId}")
    }

    private suspend fun handleInternalStop(): Boolean {
        val identifier = cloudServer?.serviceId?.toName() ?: configurationTemplate.uniqueId
        logger.fine("Detected process exit of $identifier")
        var unexpectedlyStop = false
        if (cloudServer?.connected == true) {
            unexpectedlyStop = true
            logger.warning("§cServer ${toConsoleValue(cloudServer!!.name, false)} stopped unexpectedly!")
            logger.warning("§cCheck the server logs for more information! The server directory will not be deleted!")
        }
        cloudServer!!.state = CloudServerState.STOPPED
        cloudServer!!.port = -1
        cloudServer!!.connected = false
        cloudServer!!.connectedPlayers.clear()
        serverRepository.updateServer(cloudServer!!)
        eventManager.fireEvent(CloudServerDisconnectedEvent(serverId))
        if (cloudServer!!.unregisterAfterDisconnect()) {
            serverRepository.deleteServer(cloudServer!!)
        }
        return unexpectedlyStop
    }

    private suspend fun sendStopRequestAndWait() {
        cloudServer!!.state = CloudServerState.STOPPING
        serverRepository.updateServer(cloudServer!!)
        val response = packetManager.publish(CloudServiceShutdownPacket(), cloudServer!!.serviceId)
        val answer = response.withTimeOut(4.seconds).waitBlocking()
        if (answer != null) {
            var seconds = 0
            while (cloudServer != null && cloudServer?.connected == true && seconds < SERVER_STOP_TIMEOUT) {
                delay(STOP_POLL_INTERVAL)
                seconds++
                cloudServer = serverRepository.getServer(serverId)
            }
            if (cloudServer?.connected == true) {
                logger.warning(
                    "§cServer ${toConsoleValue(
                        cloudServer!!.name,
                        false
                    )} stop request timed out. Stopping process manually!"
                )
            }
        } else {
            logger.warning(
                "§cServer ${toConsoleValue(
                    cloudServer!!.name,
                    false
                )} does not respond to stop request. Stopping process manually!"
            )
        }
    }

    private fun destroyProcess(force: Boolean) {
        if (process != null && process!!.isAlive) {
            if (force) process!!.destroyForcibly() else process!!.destroy()
        }
    }

    private fun cleanupWorkDirectory(unexpectedlyStop: Boolean) {
        if (System.getProperty("redicloud.server.delete-directory", "true").toBooleanStrictOrNull() != true) return
        if (!configurationTemplate.static && !unexpectedlyStop) {
            fileCopier.workDirectory.deleteRecursively()
        } else if (unexpectedlyStop && !configurationTemplate.static) {
            fileCopier.workDirectory.deleteOnExit()
        }
    }

    /**
     * Creates the command to start the server with based server version type configurations
     * provide also placeholders like %PORT% or %SERVICE_ID%
     */
    private fun startCommand(
        type: CloudServerVersionType,
        javaPath: String,
        snapshotData: StartDataSnapshot
    ): List<String> {
        if (!snapshotData.javaVersion.isLocated(hostServiceId)) {
            snapshotData.javaVersion.located[hostServiceId.id] = snapshotData.javaVersion.autoLocate()?.absolutePath
                ?: error("Java version ${snapshotData.javaVersion.id} not found")
        }

        val list = mutableListOf(
            javaPath,
        )

        if ((snapshotData.javaVersion.info?.major ?: -1) > JAVA_8_MAJOR_VERSION) {
            list.addAll(JPMS_OPENS)
        }
        configurationTemplate.jvmArguments.forEach { list.add(replacePlaceholders(it, snapshotData)) }
        list.add("-Xms${configurationTemplate.maxMemory}M")
        list.add("-Xmx${configurationTemplate.maxMemory}M")

        type.jvmArguments.forEach { list.add(replacePlaceholders(it, snapshotData)) }
        list.add("-jar")
        val versionHandler = IServerVersionHandler.getHandler(type)
        val jarToExecute = versionHandler.getJar(snapshotData.version)
        list.add(jarToExecute.absolutePath)
        processConfiguration!!.programParameters.forEach { list.add(replacePlaceholders(it, snapshotData)) }
        return list
    }

    fun replacePlaceholders(text: String, snapshotData: StartDataSnapshot): String =
        text.replace("%PORT%", port.toString())
            .replace("%SERVICE_ID%", cloudServer?.serviceId?.toName() ?: "unknown")
            .replace("%SERVICE_NAME%", cloudServer?.serviceId?.toName() ?: "unknown")
            .replace("%HOSTNAME%", snapshotData.hostname)
            .replace("%PROXY_SECRET%", clusterConfiguration.get("proxy-secret") ?: "redicloud_secret")
            .replace(
                "%MAX_PLAYERS%",
                (cloudServer?.maxPlayers ?: snapshotData.configurationTemplate.maxPlayers).toString()
            )
}
