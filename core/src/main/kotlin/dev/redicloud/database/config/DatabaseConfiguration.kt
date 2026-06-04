package dev.redicloud.database.config

import dev.redicloud.utils.gson.gson
import java.io.File

/**
 * Configuration for connecting to a Redis database.
 *
 * Supports both single-node and cluster mode. The mode is determined
 * automatically based on the number of [nodes] provided.
 *
 * @property username optional Redis username for authentication
 * @property password optional Redis password for authentication
 * @property nodes the list of Redis nodes to connect to
 * @property databaseId the Redis database index (single-node mode only)
 */
data class DatabaseConfiguration(
    val username: String? = "",
    val password: String? = "",
    val nodes: List<DatabaseNode>,
    val databaseId: Int = 0
) {

    /**
     * Determines whether this configuration targets a Redis cluster.
     *
     * @return true if more than one node is configured
     */
    fun isCluster(): Boolean = nodes.size > 1

    companion object {
        private const val DEFAULT_REDIS_PORT = 6379

        /**
         * Creates a [DatabaseConfiguration] from environment variables.
         *
         * Reads `RC_DATABASE_PASSWORD`, `RC_DATABASE_ID`, `RC_DATABASE_USERNAME`,
         * and `RC_DATABASE_NODES` (semicolon-separated `host:port` pairs).
         *
         * @return the configuration built from environment variables
         */
        fun fromEnv(): DatabaseConfiguration {
            val password = System.getenv("RC_DATABASE_PASSWORD") ?: ""
            val databaseId = System.getenv("RC_DATABASE_ID")?.toInt() ?: 0
            val username = System.getenv("RC_DATABASE_USERNAME") ?: ""
            val nodes = System.getenv("RC_DATABASE_NODES")?.split(";")?.map {
                val split = it.split(":")
                DatabaseNode(split[0], split[1].toInt())
            } ?: listOf(DatabaseNode("127.0.0.1", DEFAULT_REDIS_PORT))
            return DatabaseConfiguration(username, password, nodes, databaseId)
        }

        /**
         * Deserializes a [DatabaseConfiguration] from a JSON file.
         *
         * @param file the JSON file to read
         * @return the parsed configuration
         */
        fun fromFile(file: File): DatabaseConfiguration {
            return gson.fromJson(file.readText(Charsets.UTF_8), DatabaseConfiguration::class.java)
        }
    }
}

/**
 * Exports this configuration to environment variables on the given [ProcessBuilder].
 *
 * @param processBuilder the process builder whose environment will be populated
 */
fun DatabaseConfiguration.toEnv(processBuilder: ProcessBuilder) {
    processBuilder.environment()["RC_DATABASE_PASSWORD"] = password
    processBuilder.environment()["RC_DATABASE_ID"] = databaseId.toString()
    processBuilder.environment()["RC_DATABASE_NODES"] = nodes.joinToString(";") { "${it.hostname}:${it.port}" }
    processBuilder.environment()["RC_DATABASE_USERNAME"] = username
}

/**
 * Serializes this configuration to a JSON file, replacing it if it already exists.
 *
 * @param file the target file to write to
 */
fun DatabaseConfiguration.toFile(file: File) {
    if (file.exists()) file.delete()
    file.createNewFile()
    file.writeText(gson.toJson(this), Charsets.UTF_8)
}
