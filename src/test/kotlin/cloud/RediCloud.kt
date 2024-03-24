package cloud

import dev.redicloud.utils.findFreePort
import redis.RedisInstance
import java.io.File

class RediCloud(
    val name: String,
    val nodesCount: Int = 1,
    val temp: Boolean = true
) {

    companion object {
        val cloudWorkingDirectory = File("test-cloud")
    }

    val redis = RedisInstance()
    val nodes = mutableListOf<RediCloudNode>()

    init {
        addShutdownHook()
        cloudWorkingDirectory.mkdirs()
        println("Starting redis at port ${redis.port}...")
        redis.start()

        for (i in 0 until nodesCount) {
            println("Starting node-$i at debug port ${findFreePort(5000)}...")
            createNode("node-$i", name, findFreePort(5000)).start()
        }
    }

    fun createNode(name: String, cloudName: String, debugPort: Int?): RediCloudNode {
        val node = RediCloudNode(name, cloudName, debugPort, temp, cloudWorkingDirectory, redis)
        nodes.add(node)
        return node
    }

    fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            nodes.forEach { it.stop() }
            redis.stop()
        })
    }

}