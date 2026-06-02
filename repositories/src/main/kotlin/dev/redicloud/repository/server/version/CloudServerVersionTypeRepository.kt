package dev.redicloud.repository.server.version

import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.version.*
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.repository.server.version.serverversion.ServerVersion
import dev.redicloud.utils.*
import dev.redicloud.utils.gson.fromJsonToList
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.gson.gsonInterfaceFactory
import dev.redicloud.utils.withOptionalLock
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import java.util.logging.Level
import kotlin.time.Duration.Companion.minutes

@Suppress("TooManyFunctions") // Repository with CRUD + sync + download operations
class CloudServerVersionTypeRepository(
    databaseConnection: DatabaseConnection,
    private val console: Console?,
    packetManager: PacketManager,
    scope: CoroutineScope
) : CachedDatabaseBucketRepository<ICloudServerVersionType, CloudServerVersionType>(
    databaseConnection,
    "server-version-types",
    ICloudServerVersionType::class,
    CloudServerVersionType::class,
    5.minutes,
    packetManager,
    scope,
    ServiceType.NODE
),
    ICloudServerVersionTypeRepository {

    init {
        gsonInterfaceFactory.register(IServerVersion::class, ServerVersion::class)
    }

    private val locks = mutableMapOf<UUID, Mutex>()

    companion object {
        private const val ANIMATION_TICK_MS = 200L
        val LOGGER = LogManager.logger(CloudServerVersionTypeRepository::class)
        val DEFAULT_TYPES_CACHE = SingleCache(1.minutes) {
            gsonInterfaceFactory.register(IServerVersion::class, ServerVersion::class)
            val json = getTextFromGitHub("api-files/server-version-types.json")
            val list: MutableList<CloudServerVersionType> = gson.fromJsonToList<CloudServerVersionType>(
                json
            ).toMutableList()
            list.add(
                CloudServerVersionType(
                    UUID.fromString("188507b4-37b9-45b5-b977-73ed6f6192a9"),
                    "unknown",
                    "urldownloader",
                    proxy = false,
                    defaultType = true,
                    connectorPluginName = "redicloud-unknown-${CLOUD_VERSION}.jar",
                    connectorDownloadUrl = null,
                    connectorFolder = "plugins"
                )
            )
            list.toList()
        }
    }

    fun getLock(type: ICloudServerVersionType): Mutex {
        return locks.getOrPut(type.uniqueId) { Mutex() }
    }

    override suspend fun getType(name: String) = getTypes().firstOrNull { it.name.equals(name, ignoreCase = true) }

    override suspend fun getType(uniqueId: UUID) = get(uniqueId.toString())

    override suspend fun getType(version: ICloudServerVersion): ICloudServerVersionType? {
        if (version.typeId == null) return null
        return getType(version.typeId!!)
    }

    override suspend fun existsType(name: String) = getTypes().any { it.name.equals(name, ignoreCase = true) }

    override suspend fun existsType(uniqueId: UUID) = exists(uniqueId.toString())

    override suspend fun updateType(type: ICloudServerVersionType) = set(type.uniqueId.toString(), type)

    override suspend fun deleteType(type: ICloudServerVersionType): Boolean {
        return deleteType(type.uniqueId)
    }

    override suspend fun deleteType(uniqueId: UUID): Boolean {
        return delete(uniqueId.toString())
    }

    override suspend fun createType(type: ICloudServerVersionType) = set(type.uniqueId.toString(), type)

    override suspend fun getTypes(): List<CloudServerVersionType> = getAll()

    override suspend fun getOnlineTypes(): List<CloudServerVersionType> = DEFAULT_TYPES_CACHE.get() ?: emptyList()

    override suspend fun downloadConnector(serverVersionType: ICloudServerVersionType, force: Boolean, lock: Boolean) {
        val connectorFile = serverVersionType.getParsedConnectorFile(true)

        // Local-first: if connector already exists (e.g. from release zip), verify and skip download
        if (connectorFile.exists() && !force) {
            verifyConnectorIfPossible(serverVersionType, connectorFile)
            return
        }

        var canceled = false
        var downloaded = false
        var error = false
        val animation = if (console != null) {
            AnimatedLineAnimation(
                console,
                ANIMATION_TICK_MS
            ) {
                if (canceled) {
                    null
                } else if (downloaded) {
                    canceled = true
                    "Downloaded connector ${toConsoleValue(serverVersionType.name)}§8: ${if (error) "§4✘" else "§2✓"}"
                } else {
                    "Downloading connector ${toConsoleValue(serverVersionType.name)}§8: %tc%%loading%"
                }
            }
        } else {
            null
        }
        console?.startAnimation(animation!!)
        LOGGER.log(
            if (console == null) Level.INFO else Level.FINE,
            "Downloading connector for ${toConsoleValue(serverVersionType.name)}..."
        )
        getLock(serverVersionType).withOptionalLock(lock) {
            @Suppress("TooGenericExceptionCaught")
            try {
                check(
                    serverVersionType.getParsedConnectorURL().isValid()
                ) { "Connector download url of ${serverVersionType.connectorPluginName} is not reachable!" }
                httpClient.get {
                    url(serverVersionType.getParsedConnectorURL().toExternalForm())
                }.readRawBytes().let { bytes ->
                    withContext(Dispatchers.IO) {
                        if (connectorFile.exists()) connectorFile.delete()
                        connectorFile.createNewFile()
                        connectorFile.writeBytes(bytes)
                    }
                }

                // Verify integrity after download
                verifyConnectorIfPossible(serverVersionType, connectorFile)

                LOGGER.log(
                    if (console == null) Level.FINE else Level.INFO,
                    "Successfully downloaded connector for ${toConsoleValue(serverVersionType.name)}!"
                )
            } catch (e: Exception) {
                LOGGER.severe("§cFailed to download connector ${toConsoleValue(connectorFile.name, false)}!", e)
                error = true
            } finally {
                downloaded = true
            }
        }
    }

    /**
     * Verifies a connector file's SHA-256 hash if a hash is available.
     *
     * For custom connectors, the hash comes from [ICloudServerVersionType.connectorSha256].
     * For built-in connectors, the hash comes from the release manifest (looked up by filename).
     *
     * @throws IllegalStateException if a hash is available but does not match.
     */
    private fun verifyConnectorIfPossible(
        serverVersionType: ICloudServerVersionType,
        connectorFile: File
    ) {
        // 1. Check custom sha256 field (for community connectors)
        val customHash = serverVersionType.connectorSha256
        if (customHash != null) {
            check(verifyFileHash(connectorFile, customHash)) {
                "SHA-256 mismatch for connector ${connectorFile.name}: " +
                    "expected $customHash, got ${sha256(connectorFile)}"
            }
            LOGGER.info("SHA-256 verified for connector ${toConsoleValue(connectorFile.name, false)}")
            return
        }

        // 2. Check release manifest (for built-in connectors)
        val manifest = ManifestHolder.manifest
        if (manifest != null) {
            val expectedHash = manifest.findHashByFilename(connectorFile.name)
            if (expectedHash != null) {
                check(verifyFileHash(connectorFile, expectedHash)) {
                    "SHA-256 mismatch for connector ${connectorFile.name}: " +
                        "expected $expectedHash, got ${sha256(connectorFile)}"
                }
                LOGGER.info("SHA-256 verified for connector ${toConsoleValue(connectorFile.name, false)} (manifest)")
            }
        }
    }

    override suspend fun pullOnlineTypes(serverVersionRepository: ICloudServerVersionRepository, silent: Boolean) {
        val defaultTypes = getOnlineTypes()
        defaultTypes.forEach { onlineType ->
            if (onlineType.isUnknown()) return@forEach
            if (existsType(onlineType.uniqueId)) {
                updateExistingType(onlineType, serverVersionRepository, silent)
            } else {
                createType(onlineType)
                if (!silent) LOGGER.info("Pulled server version type ${toConsoleValue(onlineType.name)} from web!")
            }
        }
    }

    private suspend fun updateExistingType(
        onlineType: CloudServerVersionType,
        serverVersionRepository: ICloudServerVersionRepository,
        silent: Boolean
    ) {
        val current = getType(onlineType.uniqueId)!!
        if (current.hashCode() == onlineType.hashCode()) return
        if (!silent) LOGGER.info("Pulled server version type ${toConsoleValue(onlineType.name)} from web!")
        updateType(onlineType)
        serverVersionRepository.getVersions()
            .filter { it.typeId == current.uniqueId && current.defaultFiles != it.defaultFiles && it.used }
            .forEach { IServerVersionHandler.getHandler(current).update(it, onlineType) }
    }
}
