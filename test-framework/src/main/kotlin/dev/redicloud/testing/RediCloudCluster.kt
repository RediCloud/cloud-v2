package dev.redicloud.testing

import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.apply.impl.JarResourceLoader
import dev.redicloud.testing.config.ClusterConfiguration
import dev.redicloud.testing.config.NodeConfig
import dev.redicloud.testing.redis.RedisInstance
import org.testcontainers.containers.Network
import java.io.File

class RediCloudCluster(
    val config: ClusterConfiguration,
    val workingDirectory: File = File(WORKING_DIRECTORY, config.name),
) {

    companion object {
        val WORKING_DIRECTORY = File("testing")
    }

    val network = Network.newNetwork()
    val hostname = "redicloud.test"
    val redisInstance = RedisInstance(this)
    internal val registeredNodes = mutableListOf<RediCloudNode>()
    val nodes: List<RediCloudNode>
        get() = registeredNodes

    init {
        workingDirectory.mkdirs()
        shutdownHook()

        config.nodeConfigs.forEach {
            registerNode(it)
        }
        config.preApply(this)
        registeredNodes.forEach { it.start() }
        config.apply(this)
    }

    private fun registerNode(nodeConfig: NodeConfig): RediCloudNode {
        return RediCloudNode(nodeConfig, this).also {
            registeredNodes.add(it)
        }
    }

    fun execute(command: String): String {
        return nodes.first().execute(command)
    }

    fun unregisterNode(node: RediCloudNode) {
        node.stop()
    }

    fun unregisterNodes() {
        nodes.toSet().forEach { unregisterNode(it) }
    }

    private fun shutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            nodes.forEach {
                if (!it.tempDirectory.deleteRecursively()) {
                    it.tempDirectory.deleteOnExit()
                }
            }
        })
    }

}