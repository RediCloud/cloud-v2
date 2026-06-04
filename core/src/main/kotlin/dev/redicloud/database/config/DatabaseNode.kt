package dev.redicloud.database.config

/**
 * Represents a single Redis node endpoint.
 *
 * @property hostname the hostname or IP address of the Redis node
 * @property port the port number of the Redis node
 * @property ssl whether to use SSL/TLS for the connection
 */
data class DatabaseNode(
    val hostname: String,
    val port: Int,
    val ssl: Boolean = false
) {
    /**
     * Builds a Redis connection URI string.
     *
     * @return a URI in the format `redis://host:port` or `rediss://host:port` if SSL is enabled
     */
    fun toConnectionString(): String = "redis${if (ssl) "s" else ""}://$hostname:$port"
}
