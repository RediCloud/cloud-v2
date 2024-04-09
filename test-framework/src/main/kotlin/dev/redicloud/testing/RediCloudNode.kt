package dev.redicloud.testing

import dev.redicloud.api.utils.*
import dev.redicloud.testing.config.NodeConfig
import dev.redicloud.testing.utils.DockerUtils
import dev.redicloud.testing.utils.copyFolderContentFromContainer
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.toUUID
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile
import java.io.File
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RediCloudNode(
    val config: NodeConfig,
    val cluster: RediCloudCluster
) : GenericContainer<RediCloudNode>(
    DockerUtils.getNodeImage(cluster.config.version.branch, cluster.config.version.build)
) {

    companion object {
        internal val LOGGER = LoggerFactory.getLogger(RediCloudNode::class.java)
        val CONSOLE_COMMAND_DELAY = 20.milliseconds
    }

    val localWorkingDirectory = File(cluster.workingDirectory, config.name)
    val tempDirectory = File(RediCloudCluster.WORKING_DIRECTORY, "temp/${cluster.config.name}/${config.name}")
    val id = "${cluster.config.name}_${config.name}".toUUID()
    var terminal: Process? = null
        private set
    val localLibs = File(workingDirectory, "libs")

    init {
        tempDirectory.mkdirs()
        localWorkingDirectory.mkdirs()
        createNodeFile()
        createDatabaseFile()

        withWorkingDirectory("/app")
        withNetwork(cluster.network)
        withNetworkAliases(cluster.hostname)
        withCreateContainerCmdModifier {
            it.withName("redicloud-${cluster.config.name}-${config.name}")
            it.withTty(true)
            it.withStdinOpen(true)
            it.withAttachStdin(true)
            it.withAttachStdout(true)
            it.withAttachStderr(true)
        }
        dependsOn(cluster.redisInstance)
        withCopyToContainer(MountableFile.forHostPath(localWorkingDirectory.absolutePath), workingDirectory)
        withEnv("REDICLOUD_TESTING", "true")
    }

    fun exposeFixedPort(hostPort: Int, containerPort: Int) {
        addFixedExposedPort(containerPort, hostPort)
    }

    private fun createDatabaseFile() {
        val databaseFile = DATABASE_JSON.getFile(localWorkingDirectory)
        val config = mutableMapOf(
            "username" to "",
            "password" to "",
            "nodes" to mutableListOf(
                mutableMapOf(
                    "hostname" to cluster.redisInstance.hostname,
                    "port" to cluster.redisInstance.port,
                    "ssl" to false
                )
            )
        )
        databaseFile.writeText(gson.toJson(config))
    }

    private fun createNodeFile() {
        STORAGE_FOLDER.getFile(localWorkingDirectory).mkdirs()
        val nodeFile = NODE_JSON.getFile(localWorkingDirectory)
        val config = mutableMapOf(
            "nodeName" to config.name,
            "uniqueId" to id,
            "hostAddress" to "0.0.0.0"
        )
        nodeFile.writeText(gson.toJson(config))
    }

    override fun start() {
        LOGGER.info("Starting node {} in cluster {}", config.name, cluster.config.name)
        super.start()
        if (cluster.config.attachWithWindowsTerminal) {
            terminal = Runtime.getRuntime().exec(
                """cmd /c start "${config.name}@${cluster.config.name} | Docker-Container: $containerId" cmd /c "docker attach $containerId""""
            )
        }
        val timeout = if (localLibs.exists()) 45.seconds else 5.minutes
        waitingFor(Wait
            .forLogMessage(".*${config.name}#$id: .*(connected to the cluster)*.", 1)
            .withStartupTimeout(Duration.ofMillis(timeout.inWholeMilliseconds))
        )
        waitUntilContainerStarted()
        Thread.sleep(4000)
        execute("cluster edit ${config.name} maxMemory ${config.maxMemory}")
        config.startUpCommands.forEach { execute(it) }
        LOGGER.info("Node {} in cluster {} started", config.name, cluster.config.name)
    }

    override fun stop() {
        LOGGER.info("Stopping node {} in cluster {}", config.name, cluster.config.name)

        config.shutdownCommands.forEach { execute(it) }

        if (cluster.config.cache.cacheLibs) {
            saveCloudFileToTemplate(LIB_FOLDER)
        }

        if (cluster.config.cache.cacheConnectorJars) {
            saveCloudFileToTemplate(CONNECTORS_FOLDER)
        }

        if (cluster.config.cache.cacheServerVersionJars) {
            saveCloudFileToTemplate(MINECRAFT_VERSIONS_FOLDER)
        }

        execute("stop")
        Thread.sleep(500)
        execute("stop")
        Thread.sleep(2000)
        terminal?.destroy()

        super.stop()
        cluster.registeredNodes.remove(this)
        if (!tempDirectory.deleteRecursively()) {
            tempDirectory.deleteOnExit()
        }
    }

    fun saveCloudFileToTemplate(cloudFile: CloudFile): Boolean {
        val targetPath = "$workingDirectory/${toUniversalPath(cloudFile.getFile(), "/")}"
        return saveFileToTemplate(targetPath, cloudFile.getFile(localWorkingDirectory), cloudFile.folder)
    }

    fun saveFileToTemplate(containerPath: String, destination: File, folder: Boolean): Boolean {
        LOGGER.info("Saving folder $containerPath to ${destination.absolutePath} (${config.name}@${cluster.config.name})...")
        if (!existsFolder(containerPath)) return false
        LOGGER.info("Copying folder from container to local folder...")
        var tempFolder = tempDirectory
        tempFolder.mkdirs()
        while (tempFolder.exists()) {
            tempFolder = if (folder) {
                File(tempDirectory, destination.name + UUID.randomUUID().toString().substring(0, 5))
            } else {
                File(tempDirectory, destination.parentFile.name + UUID.randomUUID().toString().substring(0, 5))
            }
        }
        tempFolder.mkdirs()
        copyFolderContentFromContainer(containerPath, tempFolder.absolutePath)
        tempFolder.copyRecursively(destination, true)
        return true
    }

    fun existsFolder(path: String): Boolean {
        return !execInContainer("ls", path).stderr.contains("No such file or directory")
    }

    fun execute(command: String): String {
        LOGGER.info("Executing command: {}", command)
        if (!isRunning) {
            throw RuntimeException("Container is not running")
        }
        val commands = mutableListOf(
            "screen",
            "-xr",
            "redicloud",
            "-X",
            "stuff",
            "$command\\r"
        )
        try {
            val result = execInContainer(*commands.toTypedArray())
            if (result.stderr.isNotEmpty()) {
                throw RuntimeException("Failed to execute command: $commands")
            }
            return result.stdout
        } catch (e: Exception) {
            throw RuntimeException("Failed to execute command: $commands", e)
        }
    }

}