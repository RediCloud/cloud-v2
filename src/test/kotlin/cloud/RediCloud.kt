package cloud

import dev.redicloud.utils.findFreePort
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.RedisInstance
import java.io.File

class RediCloud(
    val name: String,
    val nodesCount: Int = 1,
    val temp: Boolean = true,
    val version: String
) {

    companion object {
        val cloudWorkingDirectory = File("test-cloud")
        val logger: Logger = LoggerFactory.getLogger(RediCloud::class.java)
    }

    val redis = RedisInstance(name)
    val nodes = mutableListOf<RediCloudNode>()
    var shuttingdown: Boolean = false
        private set

    init {
        logger.info("Starting cloud $name...")
        addShutdownHook()
        cloudWorkingDirectory.mkdirs()
        redis.start()
        logger.info("Started redis at port ${redis.port}...")

        for (i in 1 until nodesCount+1) {
            createNode("node-$i", name, findFreePort(5000)).start()
        }
    }

    fun createNode(name: String, cloudName: String, debugPort: Int?): RediCloudNode {
        logger.info("Creating $name in cloud $cloudName...")
        val node = runCatching { RediCloudNode(name, cloudName, temp, cloudWorkingDirectory, redis, version) }
            .getOrElse {
                throw it
            }
        nodes.add(node)
        return node
    }

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })
    }

    fun stop() {
        if (shuttingdown) {
            return
        }
        shuttingdown = true
        logger.info("Shutting down cloud $name...")
        nodes.forEach { it.stop() }
        nodes.clear()
        redis.stop()
    }

}