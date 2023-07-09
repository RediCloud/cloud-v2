package dev.redicloud.database.config

import dev.redicloud.utils.gson.gson
import java.io.File

data class DatabaseConfiguration(
    val password: String = "",
    val nodes: List<DatabaseNode>,
    val databaseId: Int = 0
) {

    fun isCluster(): Boolean = nodes.size > 1

    companion object {
        fun fromEnv(): DatabaseConfiguration {
            val password = System.getenv("RC_DATABASE_PASSWORD") ?: ""
            val databaseId = System.getenv("RC_DATABASE_ID")?.toInt() ?: 0
            val nodes = System.getenv("RC_DATABASE_NODES")?.split(";")?.map {
                val split = it.split(":")
                DatabaseNode(split[0], split[1].toInt())
            } ?: listOf(DatabaseNode("127.0.0.1", 6379))
            return DatabaseConfiguration(password, nodes, databaseId)
        }

        fun fromFile(file: File): DatabaseConfiguration {
            return gson.fromJson(file.readText(Charsets.UTF_8), DatabaseConfiguration::class.java)
        }

    }

}

fun DatabaseConfiguration.toEnv(processBuilder: ProcessBuilder) {
    processBuilder.environment()["RC_DATABASE_PASSWORD"] = password
    processBuilder.environment()["RC_DATABASE_ID"] = databaseId.toString()
    processBuilder.environment()["RC_DATABASE_NODES"] = nodes.joinToString(";") { "${it.hostname}:${it.port}" }
}

fun DatabaseConfiguration.toFile(file: File) {
    if (file.exists()) file.delete()
    file.createNewFile()
    file.writeText(gson.toJson(this), Charsets.UTF_8)
}