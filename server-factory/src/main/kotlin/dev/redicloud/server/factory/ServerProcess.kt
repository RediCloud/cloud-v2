package dev.redicloud.server.factory

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.api.service.packets.CloudServiceShutdownPacket
import dev.redicloud.logging.LogManager
import dev.redicloud.logging.getDefaultLogLevel
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.java.version.JavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.utils.CLOUD_PATH
import dev.redicloud.utils.LIB_FOLDER
import dev.redicloud.utils.findFreePort
import kotlinx.coroutines.runBlocking

class ServerProcess(
    val configurationTemplate: ConfigurationTemplate,
    private val serverRepository: ServerRepository,
    private val javaVersionRepository: JavaVersionRepository,
    private val serverVersionRepository: CloudServerVersionRepository,
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository,
    private val packetManager: PacketManager,
    private val bindHost: String
) {

    val port = findFreePort(configurationTemplate.startPort, !configurationTemplate.static)
    var process: Process? = null
    var handler: ServerProcessHandler? = null
    private val logger = LogManager.logger(ServerProcess::class)
    internal lateinit var fileCopier: FileCopier
    internal var cloudServer: CloudServer? = null

    /**
     * Starts the server process
     * @param cloudServer the cloud server instance
     */
    suspend fun start(cloudServer: CloudServer): StartResult {
        this.cloudServer = cloudServer
        cloudServer.port = port
        val processBuilder = ProcessBuilder()
        // set environment variables
        processBuilder.environment()["RC_SERVICE_ID"] = cloudServer.serviceId.toName()
        processBuilder.environment()["RC_PATH"] = CLOUD_PATH
        processBuilder.environment()["RC_HOST"] = bindHost
        processBuilder.environment()["RC_PORT"] = port.toString()
        processBuilder.environment()["RC_LOG_LEVEL"] = getDefaultLogLevel().localizedName
        processBuilder.environment()["LIBRARY_FOLDER"] = LIB_FOLDER.getFile().absolutePath
        processBuilder.environment().putAll(configurationTemplate.environments)

        if (configurationTemplate.serverVersionId == null) return UnknownServerVersionStartResult(configurationTemplate.serverVersionId)
        val serverVersion = serverVersionRepository.getVersion(configurationTemplate.serverVersionId!!)
            ?: return UnknownServerVersionStartResult(configurationTemplate.serverVersionId)

        if (serverVersion.javaVersionId == null) return UnknownJavaVersionStartResult(serverVersion.javaVersionId)
        val javaVersion = javaVersionRepository.getVersion(serverVersion.javaVersionId!!)
            ?: return UnknownJavaVersionStartResult(serverVersion.javaVersionId)

        if (serverVersion.typeId == null) return UnknownServerVersionTypeStartResult(serverVersion.typeId)
        val versionType = serverVersionTypeRepository.getType(serverVersion.typeId!!)
            ?: return UnknownServerVersionTypeStartResult(serverVersion.typeId)

        val javaPath = javaVersion.located[serverRepository.serviceId.id]
        if (javaPath.isNullOrEmpty()) return JavaVersionNotInstalledStartResult(javaVersion)

        // set command
        processBuilder.command(
            startCommand(versionType, javaVersion, javaPath)
        )
        // set working directory
        processBuilder.directory(fileCopier.workDirectory)
        process = processBuilder.start()
        // create handler and listen for exit
        handler = ServerProcessHandler(process!!, cloudServer)
        handler!!.onExit { runBlocking { stop(false) } }

        cloudServer.state = CloudServerState.STARTING
        serverRepository.updateServer(cloudServer)

        logger.fine("Started server process ${cloudServer.serviceId.toName()}")
        return SuccessStartResult(cloudServer, this)
    }

    /**
     * Stops the server process
     */
    suspend fun stop(force: Boolean = false) {
        logger.fine("Stopped server process ${configurationTemplate.uniqueId}")

        if (cloudServer != null) {
            cloudServer!!.state = CloudServerState.STOPPING
            serverRepository.updateServer(cloudServer!!)
            packetManager.publish(CloudServiceShutdownPacket(), cloudServer!!.serviceId)
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
            cloudServer!!.state = CloudServerState.STOPPED
            cloudServer!!.connected = false
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
    private fun startCommand(type: CloudServerVersionType, javaVersion: JavaVersion, javaPath: String): List<String> {
        if (!javaVersion.isLocated(serverRepository.serviceId)) {
            javaVersion.located[serverRepository.serviceId.id] = javaVersion.autoLocate()?.absolutePath
                ?: throw IllegalStateException("Java version ${javaVersion.id} not found")
        }

        val list = mutableListOf<String>(
            javaPath,
        )

        if ((javaVersion.info?.major ?: -1) > 8) {
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