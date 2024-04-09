package dev.redicloud.database.repository

import dev.redicloud.database.DatabaseConnection
import org.redisson.client.codec.BaseCodec

abstract class DatabaseRepository<T>(val connection: DatabaseConnection, val name: String) {

    init {
        if (!connection.connected) throw Exception("Database connection is not connected")
    }

    fun toDatabaseIdentifier(identifier: String): String = "${this.name}:$identifier"

}