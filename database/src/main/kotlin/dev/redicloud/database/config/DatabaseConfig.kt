package dev.redicloud.database.config

import dev.redicloud.utils.prettyPrintGson
import java.io.File

data class DatabaseConfig(
    val password: String = "",
    val nodes: List<DatabaseNode>,
    val databaseId: Int = 0
) {

    fun isCluster(): Boolean = nodes.size > 1

    companion object {
        fun fromEnv(): DatabaseConfig {
            val password = System.getenv("RC_DATABASE_PASSWORD") ?: ""
            val databaseId = System.getenv("RC_DATABASE_ID")?.toInt() ?: 0
            val nodes = System.getenv("RC_DATABASE_NODES")?.split(";")?.map {
                val split = it.split(":")
                DatabaseNode(split[0], split[1].toInt())
            } ?: listOf(DatabaseNode("127.0.0.1", 6379))
            return DatabaseConfig(password, nodes, databaseId)
        }

        fun fromFile(file: File): DatabaseConfig {
            return prettyPrintGson.fromJson(file.readText(Charsets.UTF_8), DatabaseConfig::class.java)
        }

    }

}

fun DatabaseConfig.toEnv(processBuilder: ProcessBuilder) {
    processBuilder.environment()["RC_DATABASE_PASSWORD"] = password
    processBuilder.environment()["RC_DATABASE_ID"] = databaseId.toString()
    processBuilder.environment()["RC_DATABASE_NODES"] = nodes.joinToString(";") { "${it.hostname}:${it.port}" }
}

fun DatabaseConfig.toFile(file: File) {
    file.writeText(prettyPrintGson.toJson(this), Charsets.UTF_8)
}