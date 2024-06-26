package dev.redicloud.testing.utils

import com.google.common.util.concurrent.Futures
import dev.redicloud.testing.RediCloud
import dev.redicloud.testing.pre.PreJavaVersion
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.util.concurrent.Future

val NODE_IMAGE_NAME = "redicloud-node"
val NODE_IMAGE = "redicloud-node"
val REDIS_IMAGE_NAME = "redis"

val ALPIN_IMAGE = "alpine:edge"

object DockerUtils {
    private val logger = LoggerFactory.getLogger(DockerUtils::class.java)
    fun getNodeImage(branch: String, build: String): Future<String> {
        val imageName = "$NODE_IMAGE_NAME:$branch-$build"
        try {
            val testContainer = GenericContainer(imageName)
            testContainer.start()
            testContainer.stop()
            return Futures.immediateFuture(imageName)
        }catch (e: Exception) {
            logger.info("Building image $imageName...")
            return ImageFromDockerfile(NODE_IMAGE_NAME, false)
                .withDockerfileFromBuilder { builder ->
                    val javaPackages = PreJavaVersion.values().joinToString(" ") { it.alpinPackage }
                    builder.from(ALPIN_IMAGE)
                        .workDir("/app")
                        .run("apk add screen unzip $javaPackages")
                        .run("wget https://api.redicloud.dev/v2/files/$branch/$build/redicloud.zip")
                        .run("unzip redicloud.zip")
                        .run("rm redicloud.zip start.bat")
                        .run("chmod +x start.sh")
                        .cmd("./start.sh")
                        .build()
                }
        }
    }

}