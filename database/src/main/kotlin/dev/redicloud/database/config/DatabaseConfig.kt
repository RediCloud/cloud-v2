package dev.redicloud.database.config

data class DatabaseConfig(
    val password: String = "",
    val nodes: List<DatabaseNode>,
    val databaseId: Int = 0
) {
    fun isCluster(): Boolean = nodes.size > 1
}