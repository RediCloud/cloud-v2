package dev.redicloud.repository.server.version

import com.google.gson.reflect.TypeToken
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.utils.*
import dev.redicloud.utils.gson.gson
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.logging.Level
import kotlin.time.Duration.Companion.minutes

class CloudServerVersionTypeRepository(
    databaseConnection: DatabaseConnection,
    private val console: Console?
) : DatabaseBucketRepository<CloudServerVersionType>(databaseConnection, "server-version-types") {

    private val locks = mutableMapOf<UUID, Lock>()

    companion object {
        val LOGGER = LogManager.logger(CloudServerVersionTypeRepository::class)
        val DEFAULT_TYPES_CACHE = EasyCache<List<CloudServerVersionType>, Unit>(1.minutes) {
            val json =
                khttp.get("${getAPIUrl()}/api-files/server-version-types.json").text
            val type = object : TypeToken<ArrayList<CloudServerVersionType>>() {}.type
            val list: MutableList<CloudServerVersionType> = gson.fromJson(json, type)
            if (list.none { it.isUnknown() }) {
                list.add(
                    CloudServerVersionType(
                        UUID.randomUUID(),
                        "unknown",
                        "urldownloader",
                        false,
                        true,
                        "redicloud-unknown-${CLOUD_VERSION}.jar",
                        null,
                        "plugins"
                    )
                )
            }
            list.toList()
        }
    }

    internal fun getLock(type: CloudServerVersionType): Lock {
        return locks.getOrPut(type.uniqueId) { java.util.concurrent.locks.ReentrantLock() }
    }

    suspend fun getType(name: String) = getTypes().firstOrNull { it.name.lowercase() == name.lowercase() }

    suspend fun getType(uniqueId: UUID) = get(uniqueId.toString())

    suspend fun existsType(name: String) = getTypes().any { it.name.lowercase() == name.lowercase() }

    suspend fun existsType(uniqueId: UUID) = exists(uniqueId.toString())

    suspend fun updateType(type: CloudServerVersionType) = set(type.uniqueId.toString(), type)

    suspend fun deleteType(type: CloudServerVersionType) = delete(type.uniqueId.toString())

    suspend fun createType(type: CloudServerVersionType) = set(type.uniqueId.toString(), type)

    suspend fun getTypes(): List<CloudServerVersionType> = getAll()

    suspend fun getDefaultTypes(): List<CloudServerVersionType> = DEFAULT_TYPES_CACHE.get() ?: emptyList()

    suspend fun downloadConnector(serverVersionType: CloudServerVersionType, force: Boolean = false) {
        val connectorFile = serverVersionType.getConnectorFile()
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
                    "Downloading connector %tc%${toConsoleValue(connectorFile.name)}§8: ${if (error) "§4✘" else "§2✓"}"
                } else {
                    "Downloading connector %tc%${toConsoleValue(connectorFile.name)}§8: %tc%%loading%"
                }
            }
        } else {
            null
        }
        console?.startAnimation(animation!!)
        LOGGER.log(
            if (console == null) Level.FINE else Level.INFO,
            "Downloading connector for ${serverVersionType.connectorPluginName}..."
        )
        getLock(serverVersionType).lock()
        try {
            if (!serverVersionType.getConnectorURL().isValid()) throw IllegalStateException("Connector download url of ${serverVersionType.connectorPluginName} is null!")
            khttp.get(serverVersionType.getConnectorURL().toExternalForm()).content.let {
                if (connectorFile.exists()) connectorFile.delete()
                connectorFile.createNewFile()
                connectorFile.writeBytes(it)
            }
            LOGGER.log(
                if (console == null) Level.FINE else Level.INFO,
                "Successfully downloaded connector for ${serverVersionType.connectorPluginName}!"
            )
        } catch (e: Exception) {
            LOGGER.severe("Failed to download connector for ${serverVersionType.connectorPluginName}!", e)
            error = true
        } finally {
            downloaded = true
            getLock(serverVersionType).unlock()
        }
    }

    suspend fun updateDefaultTypes(serverVersionRepository: CloudServerVersionRepository) {
        val defaultTypes = getDefaultTypes()
        defaultTypes.forEach { onlineType ->
            if (existsType(onlineType.uniqueId)) {
                val current = getType(onlineType.uniqueId)!!
                if (current == onlineType) return@forEach
                LOGGER.info("Updating default server version type ${toConsoleValue(onlineType.name)}...")
                updateType(onlineType)
                serverVersionRepository.getVersions().forEach {
                    if (it.typeId == current.uniqueId) {
                        if (current.defaultFiles != it.defaultFiles) {
                            val handler = IServerVersionHandler.getHandler(current)
                            handler.update(it)
                        }
                    }
                }
                return@forEach
            }
            createType(onlineType)
        }
    }

}