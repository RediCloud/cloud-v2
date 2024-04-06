package cloud

import dev.redicloud.utils.findFreePort
import org.slf4j.LoggerFactory
import redis.RedisInstance
import java.io.File

class RediCloudNode(
    val name: String,
    val cloudName: String,
    val temp: Boolean = true,
    cloudWorkingDirectory: File,
    val redis: RedisInstance,
    val version: String
) {

    companion object {
        val logger = LoggerFactory.getLogger(RediCloudNode::class.java)
    }

    val workingDirectory = File(cloudWorkingDirectory, "$cloudName/$name")
    val environmentLoader = EnvironmentLoader()
    val fileCopier = CloudFileCopier(workingDirectory, version, cloudName, name)
    val process = CloudProcess(fileCopier, environmentLoader, cloudName, name)

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