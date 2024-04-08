package dev.redicloud.testing

import dev.redicloud.api.utils.DATABASE_JSON
import dev.redicloud.api.utils.NODE_JSON
import dev.redicloud.api.utils.STORAGE_FOLDER
import dev.redicloud.testing.utils.DockerUtils
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.toUUID
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile
import java.io.File

class RediCloudNode(
    val name: String,
    val cluster: RediCloudCluster
) : GenericContainer<RediCloudNode>(
    DockerUtils.getNodeImage(cluster.config.version.branch, cluster.config.version.build)
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RediCloudNode::class.java)
    }

    val workingDirectory = File(cluster.workingDirectory, name)
    val tempDirectory = File(RediCloudCluster.WORKING_DIRECTORY, "tmp/${cluster.config.name}/$name")
    val id = "${cluster.config.name}_${name}".toUUID()
    var terminal: Process? = null
        private set

    init {
        tempDirectory.mkdirs()
        workingDirectory.mkdirs()
        createNodeFile()
        createDatabaseFile()

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
        withCopyToContainer(MountableFile.forHostPath(workingDirectory.absolutePath), "/app")
        if (RediCloudCluster.LIB_FOLDER.isDirectory) {
            withCopyToContainer(
                MountableFile.forHostPath(RediCloudCluster.LIB_FOLDER.absolutePath),
                "/libs"
            )
        }
        withEnv("LIBRARY_FOLDER", "/libs")
        withEnv("JAVA_INSTALLATIONS_FOLDER", "/opt")
        withEnv("REDICLOUD_TESTING", "true")
    }

    private fun createDatabaseFile() {
        val databaseFile = DATABASE_JSON.getFile(workingDirectory)
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
        STORAGE_FOLDER.getFile(workingDirectory).mkdirs()
        val nodeFile = NODE_JSON.getFile(workingDirectory)
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
        waitingFor(Wait.forLogMessage(".*${name}#$id: .*(connected to the cluster)*.", 1))
        waitUntilContainerStarted()
        Thread.sleep(4000)
        logger.info("Node {} in cluster {} started", name, cluster.config.name)

    }

    override fun stop() {
        logger.info("Stopping node {} in cluster {}", name, cluster.config.name)
        execute("stop")
        Thread.sleep(500)
        execute("stop")
        Thread.sleep(2000)
        terminal?.destroy()
        logger.info("Saving libs to {}", RediCloudCluster.LIB_FOLDER)
        val tempLibs = File(tempDirectory, "libs")
        copyFileFromContainer("/libs", tempLibs.absolutePath)
        tempLibs.listFiles()?.forEach {
            it.copyTo(File(RediCloudCluster.LIB_FOLDER, it.name), true)
        }
        super.stop()
        cluster.registeredNodes.remove(this)
        if (!workingDirectory.deleteRecursively()) {
            workingDirectory.deleteOnExit()
        }
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