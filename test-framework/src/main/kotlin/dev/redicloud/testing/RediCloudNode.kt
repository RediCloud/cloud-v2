package dev.redicloud.testing

import dev.redicloud.api.utils.*
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RediCloudNode(
    val name: String,
    val cluster: RediCloudCluster
) : GenericContainer<RediCloudNode>(
    DockerUtils.getNodeImage(cluster.config.version.branch, cluster.config.version.build)
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RediCloudNode::class.java)
    }

    val localWorkingDirectory = File(cluster.workingDirectory, name)
    val tempDirectory = File(RediCloudCluster.WORKING_DIRECTORY, "temp/${cluster.config.name}/$name")
    val id = "${cluster.config.name}_${name}".toUUID()
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
            it.withName("redicloud-${cluster.config.name}-$name")
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
            "nodeName" to name,
            "uniqueId" to id,
            "hostAddress" to "127.0.0.1"
        )
        nodeFile.writeText(gson.toJson(config))
    }

    override fun start() {
        logger.info("Starting node {} in cluster {}", name, cluster.config.name)
        super.start()
        if (cluster.config.attachWithWindowsTerminal) {
            terminal = Runtime.getRuntime().exec(
                """cmd /c start "$name@${cluster.config.name} | Docker-Container: $containerId" cmd /c "docker attach $containerId""""
            )
        }
        val timeout = if (localLibs.exists()) 45.seconds else 5.minutes
        waitingFor(Wait
            .forLogMessage(".*${name}#$id: .*(connected to the cluster)*.", 1)
            .withStartupTimeout(Duration.ofMillis(timeout.inWholeMilliseconds))
        )
        waitUntilContainerStarted()
        Thread.sleep(4000)
        logger.info("Node {} in cluster {} started", name, cluster.config.name)

    }

    override fun stop() {
        logger.info("Stopping node {} in cluster {}", name, cluster.config.name)

        if (cluster.config.cache.cacheLibs) {
            val targetPath = "$workingDirectory/${toUniversalPath(LIB_FOLDER.getFile(), "/")}"
            if (!execInContainer("ls", targetPath).stderr.contains("No such file or directory")) {
                logger.info("Copying libs from container to local folder...")
                val tempLibs = File(tempDirectory, "libs")
                tempLibs.deleteRecursively()
                copyFolderContentFromContainer(targetPath, tempLibs.absolutePath)
                tempLibs.copyRecursively(LIB_FOLDER.getFile(localWorkingDirectory), true)
            }
        }

        if (cluster.config.cache.cacheConnectorJars) {
            val targetPath = "$workingDirectory/${toUniversalPath(CONNECTORS_FOLDER.getFile(), "/")}"
            if (!execInContainer("ls", targetPath).stderr.contains("No such file or directory")) {
                logger.info("Copying connector jars from container to local folder...")
                val tempConnectors = File(tempDirectory, "connectors")
                tempConnectors.deleteRecursively()
                copyFolderContentFromContainer(targetPath, tempConnectors.absolutePath)
                tempConnectors.copyRecursively(CONNECTORS_FOLDER.getFile(localWorkingDirectory), true)
            }
        }

        if (cluster.config.cache.cacheServerVersionJars) {
            val targetPath = "$workingDirectory/${toUniversalPath(MINECRAFT_VERSIONS_FOLDER.getFile(), "/")}"
            if (!execInContainer("ls", targetPath).stderr.contains("No such file or directory")) {
                logger.info("Copying server version jars from container to local folder...")
                val tempServerVersions = File(tempDirectory, "server-versions")
                tempServerVersions.deleteRecursively()
                copyFolderContentFromContainer(targetPath, tempServerVersions.absolutePath)
                tempServerVersions.copyRecursively(MINECRAFT_VERSIONS_FOLDER.getFile(localWorkingDirectory), true)
            }
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

    fun execute(command: String): String {
        logger.info("Executing command: {}", command)
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