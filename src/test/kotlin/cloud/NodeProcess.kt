package cloud

import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.MountableFile
import java.io.File

class NodeProcess(
    cloudFileCopier: CloudFileCopier,
    environmentLoader: EnvironmentLoader,
    cloudName: String,
    cloudNodeName: String,
    network: Network
) {

    companion object {
        val logger = LoggerFactory.getLogger(NodeProcess::class.java)
    }

    private val container: GenericContainer<*>
    val debugPort: Int
        get() = container.getMappedPort(5000)

    private val screenName = "redicloud-node"

    init {
        val java = System.getProperty("redicloud.java", "java")

        var command = cloudFileCopier.startFile.readText()
        command = command
            .replace("%java%", java)
            .replace("%cloud_name%", cloudName)
            .replace("%cloud_node_name%", cloudNodeName)
        command = "screen -S $screenName $command"
        container = GenericContainer(NODE_IMAGE)
            .withCopyFileToContainer(MountableFile.forHostPath(cloudFileCopier.workingDirectory.absolutePath), "/app")
            .withCopyFileToContainer(
                MountableFile.forHostPath(cloudFileCopier.workingDirectory.parentFile.parentFile.absolutePath + File.separator + ".libs"),
                "/libs"
            )
            .withWorkingDirectory("/app")
            .withExposedPorts(5000)
            .withCreateContainerCmdModifier {
                it.withName("redicloud-$cloudName-$cloudNodeName")
                it.withTty(true)
                it.withStdinOpen(true)
                it.withCmd(*splitCommand(command).toTypedArray())
                it.withAttachStdin(true)
                it.withAttachStdout(true)
                it.withAttachStderr(true)
            }
            .withNetwork(network)
            .withNetworkAliases("cluster.redicloud.test")
        environmentLoader.environments().forEach { (key, value) ->
            container.withEnv(key, value)
        }
    }

    private fun splitCommand(command: String): List<String> {
        val parts = mutableListOf<String>()
        val regex = """[^\s"]+|"([^"]*)"""".toRegex()
        regex.findAll(command).forEach { matchResult ->
            matchResult.groupValues.forEach {
                if (it.isNotBlank()) {
                    parts.add(it)
                }
            }
        }
        return parts
    }

    fun isRunning(): Boolean {
        return container.isRunning
    }

    fun start() {
        logger.info("Starting node container...")
        container.start()
    }

    fun stop() {
        logger.info("Stopping process...")
        if (!isRunning()) {
            return
        }
        execute("stop")
        Thread.sleep(200)
        execute("stop")
        var count = 0
        while (isRunning() && count < 20) {
            Thread.sleep(500)
            count++
        }
        if (isRunning()) {
            logger.warn("Process did not stop in time. Destroying it.")
            container.stop()
        }
    }

    fun execute(command: String): String {
        logger.info("Executing command: {}", command)
        if (!container.isRunning) {
            throw RuntimeException("Container is not running")
        }
        val commands = mutableListOf(
            "screen",
            "-xr",
            screenName,
            "-X",
            "stuff",
            "$command\\r"
        )
        try {
            logger.info("Executing command: {}", commands)
            val result = container.execInContainer(*commands.toTypedArray())
            if (result.stderr.isNotEmpty()) {
                throw RuntimeException("Failed to execute command: $commands")
            }
            return result.stdout
        } catch (e: Exception) {
            throw RuntimeException("Failed to execute command: $commands", e)
        }
    }

}