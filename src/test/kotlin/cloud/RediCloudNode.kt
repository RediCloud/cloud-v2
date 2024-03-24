package cloud

import dev.redicloud.utils.findFreePort
import redis.RedisInstance
import java.io.File

class RediCloudNode(
    val name: String,
    val cloudName: String,
    val debugPort: Int?,
    val temp: Boolean = true,
    cloudWorkingDirectory: File,
    val redis: RedisInstance
) {

    val workingDirectory = File(cloudWorkingDirectory, name)
    val environmentLoader = EnvironmentLoader()
    val fileCopier = CloudFileCopier(workingDirectory)
    val process = CloudProcess(fileCopier, debugPort, environmentLoader)

    init {
        workingDirectory.mkdirs()
        fileCopier.createNodeFile(name, cloudName)
        fileCopier.createDatabaseFile(redis)
    }

    fun start() {
        process.start()
    }

    fun stop() {
        process.stop()
        if (temp) {
            workingDirectory.deleteRecursively()
        }
    }

}