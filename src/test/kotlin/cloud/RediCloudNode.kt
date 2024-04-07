package cloud

import org.slf4j.LoggerFactory
import org.testcontainers.containers.Network
import redis.RedisInstance
import java.io.File

class RediCloudNode(
    val name: String,
    val cloudName: String,
    val temp: Boolean = true,
    cloudWorkingDirectory: File,
    redis: RedisInstance,
    version: String,
    network: Network
) {

    companion object {
        val logger = LoggerFactory.getLogger(RediCloudNode::class.java)
    }

    val workingDirectory = File(cloudWorkingDirectory, "$cloudName/$name")
    val environmentLoader = EnvironmentLoader()
    val fileCopier = CloudFileCopier(workingDirectory, version, cloudName, name)
    val process = NodeProcess(fileCopier, environmentLoader, cloudName, name, network)

    init {
        workingDirectory.mkdirs()
        fileCopier.createNodeFile(name, cloudName)
        fileCopier.createDatabaseFile(redis)
    }

    fun start() {
        process.start()
    }

    fun stop() {
        logger.info("Shutting down node $name in cloud $cloudName...")
        process.stop()
        if (temp) {
            workingDirectory.deleteRecursively()
        }
    }

}