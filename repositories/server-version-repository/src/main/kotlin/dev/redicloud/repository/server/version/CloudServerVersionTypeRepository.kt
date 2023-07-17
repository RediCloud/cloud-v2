package dev.redicloud.repository.server.version

import com.google.gson.reflect.TypeToken
import dev.redicloud.api.repositories.version.*
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.*
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.gson.gsonInterfaceFactory
import dev.redicloud.utils.service.ServiceType
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
    null,
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
            val type = object : TypeToken<ArrayList<CloudServerVersionType>>() {}.type
            val list: MutableList<CloudServerVersionType> = gson.fromJson(json, type)
            list.add(
                CloudServerVersionType(
                    UUID.fromString("188507b4-37b9-45b5-b977-73ed6f6192a9"),
                    "unknown",
                    "urldownloader",
                    false,
                    true,
                    "redicloud-unknown-${CLOUD_VERSION}.jar",
                    null,
                    "plugins"
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
                    "Downloading connector ${toConsoleValue(connectorFile.name)}§8: ${if (error) "§4✘" else "§2✓"}"
                } else {
                    "Downloading connector ${toConsoleValue(connectorFile.name)}§8: %tc%%loading%"
                }
            }
        } else {
            null
        }
        console?.startAnimation(animation!!)
        LOGGER.log(
            if (console == null) Level.INFO else Level.FINE,
            "Downloading connector for ${toConsoleValue(connectorFile.name)}..."
        )
        if (lock) getLock(serverVersionType).lock()
        try {
            if (!serverVersionType.getParsedConnectorURL().isValid()) throw IllegalStateException("Connector download url of ${serverVersionType.connectorPluginName} is null!")
            khttp.get(serverVersionType.getParsedConnectorURL().toExternalForm()).content.let {
                if (connectorFile.exists()) connectorFile.delete()
                connectorFile.createNewFile()
                connectorFile.writeBytes(it)
            }
            LOGGER.log(
                if (console == null) Level.FINE else Level.INFO,
                "Successfully downloaded connector for ${toConsoleValue(connectorFile.name)}!"
            )
        } catch (e: Exception) {
            LOGGER.severe("§cFailed to download connector for ${toConsoleValue(connectorFile.name, false)}!", e)
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
                if (current == onlineType) return@forEach
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