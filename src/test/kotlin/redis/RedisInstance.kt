package redis

import dev.redicloud.utils.findFreePort
import org.testcontainers.containers.GenericContainer

class RedisInstance(
    val identifier: String,
    val version: String = "7.2.4"
) {

    private val container = GenericContainer("redis:$version")
        .withExposedPorts(6379)
        .withCreateContainerCmdModifier {
            it.withName("redicloud-$identifier-redis")
        }
    val port: Int
        get() = container.getMappedPort(6379)
    val uri: String
        get() = "redis://127.0.0.1:$port"

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