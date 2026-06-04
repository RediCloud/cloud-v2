package dev.redicloud.database.repository

import dev.redicloud.api.database.IDatabaseConnection

/**
 * Base class for database repositories that operate on a named key prefix.
 *
 * Validates that the database connection is active upon construction and provides
 * a helper to build fully-qualified database identifiers.
 *
 * @param T the type of entity managed by this repository
 * @param connection the active database connection
 * @param name the key prefix used to namespace entries in the database
 */
abstract class DatabaseRepository<T>(
    val connection: IDatabaseConnection,
    val name: String
) {

    init {
        if (!connection.connected) error("Database connection is not connected")
    }

    /**
     * Creates a fully-qualified database key by combining the repository name with the given identifier.
     *
     * @param identifier the entity-specific identifier
     * @return the namespaced database key in the format `name:identifier`
     */
    fun toDatabaseIdentifier(identifier: String): String = "${this.name}:$identifier"
}
