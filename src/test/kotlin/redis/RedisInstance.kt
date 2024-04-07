package redis

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network

class RedisInstance(
    identifier: String,
    network: Network,
    version: String = "7.2.4"
) {

    val port = 6379
    val hostname = "redis.redicloud.test"
    private val container = GenericContainer("redis:$version")
        .withCreateContainerCmdModifier {
            it.withName("redicloud-$identifier-redis")
        }
        .withNetwork(network)
        .withNetworkAliases(hostname)
    val uri: String
        get() {
            return "redis://$hostname:$port"
        }

    fun start() {
        container.start()
    }

    fun stop() {
        if (!container.isRunning) {
            return
        }
        execute("shutdown")
    }

    fun execute(vararg commands: String): String {
        if (!container.isRunning) {
            throw RuntimeException("Container is not running")
        }
        val c = mutableListOf("redis-cli").also { it.addAll(commands) }
        try {
            val result = container.execInContainer(*c.toTypedArray())
            if (result.stderr.isNotEmpty()) {
                throw RuntimeException("Failed to execute command: $commands")
            }
            return result.stdout
        } catch (e: Exception) {
            throw RuntimeException("Failed to execute command: $commands", e)
        }
    }

}