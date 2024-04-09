package dev.redicloud.testing.redis

import dev.redicloud.testing.RediCloudCluster
import dev.redicloud.testing.utils.REDIS_IMAGE_NAME
import org.testcontainers.containers.GenericContainer

class RedisInstance(
    cluster: RediCloudCluster,
    version: String = "7.2.4"
) : GenericContainer<RedisInstance>("$REDIS_IMAGE_NAME:$version") {

    val port = 6379
    val hostname = "redis.redicloud.test"
    val uri = "redis://$cluster.hostname:$port"

    init {
        withCreateContainerCmdModifier {
            it.withName("redicloud-${cluster.config.name}-redis")
            it.withHostName(hostname)
        }
        withNetwork(cluster.network)
        withNetworkAliases(cluster.hostname)
        if (cluster.config.exposeRedis) {
            withExposedPorts(port)
        }
    }


    fun execute(vararg commands: String): String {
        if (!isRunning) {
            throw RuntimeException("Container is not running")
        }
        val c = mutableListOf("redis-cli").also { it.addAll(commands) }
        try {
            val result = execInContainer(*c.toTypedArray())
            if (result.stderr.isNotEmpty()) {
                throw RuntimeException("Failed to execute command: $commands")
            }
            return result.stdout
        } catch (e: Exception) {
            throw RuntimeException("Failed to execute command: $commands", e)
        }
    }

}