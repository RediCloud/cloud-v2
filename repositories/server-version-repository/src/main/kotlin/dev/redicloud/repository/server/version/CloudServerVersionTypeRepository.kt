package dev.redicloud.repository.server.version

import com.google.gson.reflect.TypeToken
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
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.gson.gsonInterfaceFactory
import dev.redicloud.api.service.ServiceType
import dev.redicloud.utils.gson.fromJsonToList
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import kotlin.time.Duration.Companion.minutes

class CloudServerVersionTypeRepository(
    databaseConnection: DatabaseConnection,
    private val console: Console?,
    packetManager: PacketManager
) : CachedDatabaseBucketRepository<ICloudServerVersionType, CloudServerVersionType>(
    databaseConnection,
    "server-version-types",
    ICloudServerVersionType::class,
    CloudServerVersionType::class,
    5.minutes,
    packetManager,
    ServiceType.NODE
), ICloudServerVersionTypeRepository {

    init {
        gsonInterfaceFactory.register(IServerVersion::class, ServerVersion::class)
    }

    private val locks = mutableMapOf<UUID, ReentrantLock>()

    companion object {
        val LOGGER = LogManager.logger(CloudServerVersionTypeRepository::class)
        val DEFAULT_TYPES_CACHE = SingleCache(1.minutes) {
            gsonInterfaceFactory.register(IServerVersion::class, ServerVersion::class)
            val json = getTextOfAPIWithFallback("api-files/server-version-types.json")
            val list: MutableList<CloudServerVersionType> = gson.fromJsonToList<CloudServerVersionType>(json).toMutableList()
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

    fun getLock(type: ICloudServerVersionType): ReentrantLock {
        return locks.getOrPut(type.uniqueId) { java.util.concurrent.locks.ReentrantLock() }
    }

    override suspend fun getType(name: String) = getTypes().firstOrNull { it.name.lowercase() == name.lowercase() }

    override suspend fun getType(uniqueId: UUID) = get(uniqueId.toString())

    override suspend fun getType(version: ICloudServerVersion): ICloudServerVersionType? {
        if (version.typeId == null) return null
        return getType(version.typeId!!)
    }

    override suspend fun existsType(name: String) = getTypes().any { it.name.lowercase() == name.lowercase() }

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
        if (connectorFile.exists() && !force) return
        var canceled = false
        var downloaded = false
        var error = false
        val animation = if (console != null) {
            AnimatedLineAnimation(
                console,
                200
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
        if (lock) getLock(serverVersionType).lock()
        try {
            if (!serverVersionType.getParsedConnectorURL().isValid()) throw IllegalStateException("Connector download url of ${serverVersionType.connectorPluginName} is null!")
            httpClient.get {
                url(serverVersionType.getParsedConnectorURL().toExternalForm())
            }.readBytes().let {
                if (connectorFile.exists()) connectorFile.delete()
                connectorFile.createNewFile()
                connectorFile.writeBytes(it)
            }
            LOGGER.log(
                if (console == null) Level.FINE else Level.INFO,
                "Successfully downloaded connector for ${toConsoleValue(serverVersionType.name)}!"
            )
        } catch (e: Exception) {
            LOGGER.severe("§cFailed to download connector ${toConsoleValue(connectorFile.name, false)}!", e)
            error = true
        } finally {
            downloaded = true
            if (lock) getLock(serverVersionType).unlock()
        }
    }

    override suspend fun pullOnlineTypes(serverVersionRepository: ICloudServerVersionRepository, silent: Boolean) {

        val defaultTypes = getOnlineTypes()
        defaultTypes.forEach { onlineType ->
            if (onlineType.isUnknown()) return@forEach
            if (existsType(onlineType.uniqueId)) {
                val current = getType(onlineType.uniqueId)!!
                if (current.hashCode() == onlineType.hashCode()) return@forEach
                if (!silent) LOGGER.info("Pulled server version type ${toConsoleValue(onlineType.name)} from web!")
                updateType(onlineType)
                serverVersionRepository.getVersions().forEach {
                    if (it.typeId == current.uniqueId) {
                        if (current.defaultFiles != it.defaultFiles && it.used) {
                            val handler = IServerVersionHandler.getHandler(current)
                            handler.update(it, onlineType)
                        }
                    }
                }
                return@forEach
            }
            createType(onlineType)
            if (!silent) LOGGER.info("Pulled server version type ${toConsoleValue(onlineType.name)} from web!")
        }
    }

}