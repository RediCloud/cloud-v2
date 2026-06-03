package dev.redicloud.migration

import dev.redicloud.api.database.IDatabaseConnection
import java.io.File

/**
 * Context passed to each migration, providing access to the database and the local working directory.
 */
data class MigrationContext(
    val databaseConnection: IDatabaseConnection,
    val workingDirectory: File
)
