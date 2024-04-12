package dev.redicloud.database.repository

import dev.redicloud.api.database.IDatabaseConnection

abstract class DatabaseRepository<T>(
    val connection: IDatabaseConnection,
    val name: String
)  {

    init {
        if (!connection.connected) throw Exception("Database connection is not connected")
    }

    fun toDatabaseIdentifier(identifier: String): String = "${this.name}:$identifier"

}