package dev.redicloud.testing

import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.apply.impl.JarResourceLoader
import dev.redicloud.testing.config.ClusterConfiguration
import dev.redicloud.testing.redis.RedisInstance
import org.testcontainers.containers.Network
import java.io.File

class RediCloudCluster(
    val config: ClusterConfiguration,
    val workingDirectory: File = File(WORKING_DIRECTORY, config.name),
) {

    companion object {
        val WORKING_DIRECTORY = File("testing")
        val LIB_FOLDER = File(WORKING_DIRECTORY, "libs")
    }

    val network = Network.newNetwork()
    val hostname = "redicloud.test"
    val redisInstance = RedisInstance(this)
    internal val registeredNodes = mutableListOf<RediCloudNode>()
    val nodes: List<RediCloudNode>
        get() = registeredNodes

    init {
        workingDirectory.mkdirs()

        for (i in 1 until config.nodes+1) {
            registerNode("node-$i")
        }
        config.preApply(this)
        registeredNodes.forEach { it.start() }
        config.apply(this)
    }

    private fun registerNode(name: String): RediCloudNode {
        return RediCloudNode(name, this).also {
            registeredNodes.add(it)
        }
    }

    fun unregisterNode(node: RediCloudNode) {
        node.stop()
    }

    fun unregisterNodes() {
        nodes.toSet().forEach { unregisterNode(it) }
    }

    private fun shutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            if (!workingDirectory.deleteRecursively()) {
                workingDirectory.deleteOnExit()
            }
            nodes.forEach {
                if (!it.tempDirectory.deleteRecursively()) {
                    it.tempDirectory.deleteOnExit()
                }
            }
        })
    }

}