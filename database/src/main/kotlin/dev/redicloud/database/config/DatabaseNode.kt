package dev.redicloud.database.config

data class DatabaseNode(
    val hostname: String,
    val port: Int,
    val ssl: Boolean = false
) {
    fun toConnectionString(): String = "redis${if (ssl) "s" else ""}://$hostname:$port"
}