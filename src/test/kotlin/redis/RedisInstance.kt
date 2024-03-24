package redis

import dev.redicloud.utils.findFreePort
import org.testcontainers.containers.GenericContainer

class RedisInstance(
    val version: String = "7.2.4",
    val port: Int = findFreePort(6379)
) {

    private val container = GenericContainer("redis:$version")
        .withExposedPorts(port)
    val uri = "redis://127.0.0.1:$port"

    fun start() {
        container.start()
    }

    fun stop() {
        execute("shutdown")
    }

    fun execute(vararg commands: String): String {
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