package dev.redicloud.database.repository

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import org.redisson.client.codec.BaseCodec

abstract class DatabaseRepository<T>(val connection: DatabaseConnection, val name: String, val codec: BaseCodec? = null) {

    init {
        if (!connection.isConnected()) throw Exception("Database connection is not connected")
    }

    fun toDatabaseIdentifier(identifier: String): String = "${this.name}:$identifier"

}