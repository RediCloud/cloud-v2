package dev.redicloud.migration

import dev.redicloud.api.database.IDatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.version.CloudVersion
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Executes pending migrations on startup.
 *
 * - [MigrationType.DATABASE] migrations run **once** across the cluster, protected by a distributed lock.
 * - [MigrationType.LOCAL] migrations run **on every node** independently.
 *
 * Must be called after the database connection is established but before the node registers as connected.
 */
class MigrationRunner(
    private val databaseConnection: IDatabaseConnection,
    private val workingDirectory: File = File("."),
    private val lockTimeout: Duration = LOCK_TIMEOUT,
    private val waitTimeout: Duration = WAIT_TIMEOUT,
    private val waitPollInterval: Duration = WAIT_POLL_INTERVAL
) {

    companion object {
        private val LOGGER = LogManager.logger(MigrationRunner::class)

        private const val SCHEMA_VERSION_KEY = "cloud:schema-version"
        private const val MIGRATION_LOCK_KEY = "cloud:migration-lock"
        private const val LOCAL_SCHEMA_VERSION_FILE = ".local-schema-version"

        private val LOCK_TIMEOUT = 5.minutes
        private val WAIT_TIMEOUT = 10.minutes
        private val WAIT_POLL_INTERVAL = 2.seconds
    }

    /**
     * Runs all pending migrations.
     *
     * @param currentVersion the version of the running JAR (from build properties)
     * @throws IllegalStateException if migrations fail or the wait-state times out
     */
    suspend fun run(currentVersion: CloudVersion) {
        val context = MigrationContext(databaseConnection, workingDirectory)

        runDatabaseMigrations(currentVersion, context)
        runLocalMigrations(currentVersion, context)
    }

    // ------------------------------------------------------------------
    // Database Migrations (once per cluster, with distributed lock)
    // ------------------------------------------------------------------

    private suspend fun runDatabaseMigrations(currentVersion: CloudVersion, context: MigrationContext) {
        val storedVersion = getSchemaVersion()

        if (storedVersion == null) {
            // First run ever -- set schema version to current, no migrations needed
            LOGGER.info("No schema version found. Initializing to ${currentVersion.display}")
            setSchemaVersion(currentVersion)
            return
        }

        if (storedVersion == currentVersion) {
            LOGGER.info("Database schema is up to date (${currentVersion.display})")
            return
        }

        val isUpgrade = storedVersion < currentVersion
        val allMigrations = MigrationRegistry.getMigrationsBetween(storedVersion, currentVersion)
        val dbMigrations = allMigrations.filter { it.type == MigrationType.DATABASE }

        if (dbMigrations.isEmpty()) {
            LOGGER.info("No database migrations needed ($storedVersion -> $currentVersion)")
            setSchemaVersion(currentVersion)
            return
        }

        val direction = if (isUpgrade) "Upgrading" else "Downgrading"
        LOGGER.info("$direction database: $storedVersion -> $currentVersion (${dbMigrations.size} migrations)")

        val lock = databaseConnection.getLock(MIGRATION_LOCK_KEY)

        // Try to acquire the lock
        if (lock.isLocked() && !lock.isHeldByCurrentThread()) {
            LOGGER.info("Another node is running database migrations. Waiting...")
            waitForMigrationComplete(currentVersion)
            return
        }

        lock.lock(lockTimeout)
        try {
            // Re-check schema version -- another node may have migrated while we waited for the lock
            val recheckVersion = getSchemaVersion()
            if (recheckVersion == currentVersion) {
                LOGGER.info("Database schema was already migrated by another node")
                return
            }

            for (migration in dbMigrations) {
                LOGGER.info("Running database migration: ${migration.description} (-> ${migration.version})")
                @Suppress("TooGenericExceptionCaught")
                try {
                    if (isUpgrade) migration.up(context) else migration.down(context)
                } catch (e: Exception) {
                    LOGGER.severe("Database migration failed: ${migration.description}", e)
                    throw IllegalStateException(
                        "Database migration failed: ${migration.description}. " +
                            "The cluster may be in a partially migrated state. Manual intervention required.",
                        e
                    )
                }
            }

            setSchemaVersion(currentVersion)
            LOGGER.info("Database migrations complete. Schema version: $currentVersion")
        } finally {
            lock.unlock()
        }
    }

    /**
     * Wait-state: polls until the schema version matches or timeout is reached.
     */
    private suspend fun waitForMigrationComplete(expectedVersion: CloudVersion) {
        val deadline = System.currentTimeMillis() + waitTimeout.inWholeMilliseconds
        while (System.currentTimeMillis() < deadline) {
            delay(waitPollInterval)
            val currentSchema = getSchemaVersion()
            if (currentSchema == expectedVersion) {
                LOGGER.info("Database migration completed by another node. Continuing startup.")
                return
            }
        }
        throw IllegalStateException(
            "Timed out waiting for database migration to complete " +
                "(expected schema version: $expectedVersion). " +
                "Check if the migrating node is still running."
        )
    }

    // ------------------------------------------------------------------
    // Local Migrations (per node, file/folder changes)
    // ------------------------------------------------------------------

    private suspend fun runLocalMigrations(currentVersion: CloudVersion, context: MigrationContext) {
        val localVersionFile = File(workingDirectory, LOCAL_SCHEMA_VERSION_FILE)
        val storedLocal = if (localVersionFile.exists()) {
            CloudVersion.parseOrNull(localVersionFile.readText().trim())
        } else {
            null
        }

        if (storedLocal == null) {
            // First run -- initialize
            localVersionFile.writeText(currentVersion.display)
            return
        }

        if (storedLocal == currentVersion) return

        val isUpgrade = storedLocal < currentVersion
        val allMigrations = MigrationRegistry.getMigrationsBetween(storedLocal, currentVersion)
        val localMigrations = allMigrations.filter { it.type == MigrationType.LOCAL }

        if (localMigrations.isEmpty()) {
            localVersionFile.writeText(currentVersion.display)
            return
        }

        val direction = if (isUpgrade) "Upgrading" else "Downgrading"
        LOGGER.info("$direction local files: $storedLocal -> $currentVersion (${localMigrations.size} migrations)")

        for (migration in localMigrations) {
            LOGGER.info("Running local migration: ${migration.description} (-> ${migration.version})")
            @Suppress("TooGenericExceptionCaught")
            try {
                if (isUpgrade) migration.up(context) else migration.down(context)
            } catch (e: Exception) {
                LOGGER.severe("Local migration failed: ${migration.description}", e)
                throw IllegalStateException(
                    "Local migration failed: ${migration.description}. Manual intervention required.",
                    e
                )
            }
        }

        localVersionFile.writeText(currentVersion.display)
        LOGGER.info("Local migrations complete.")
    }

    // ------------------------------------------------------------------
    // Schema version helpers
    // ------------------------------------------------------------------

    private suspend fun getSchemaVersion(): CloudVersion? {
        val bucket = databaseConnection.getBucket<String>(SCHEMA_VERSION_KEY)
        val raw = bucket.get() ?: return null
        return CloudVersion.parseOrNull(raw)
    }

    private suspend fun setSchemaVersion(version: CloudVersion) {
        val bucket = databaseConnection.getBucket<String>(SCHEMA_VERSION_KEY)
        bucket.set(version.display)
    }
}
