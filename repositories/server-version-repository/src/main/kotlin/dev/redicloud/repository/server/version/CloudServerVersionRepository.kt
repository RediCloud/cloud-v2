package dev.redicloud.repository.server.version

import com.google.gson.reflect.TypeToken
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.utils.CLOUD_VERSION
import dev.redicloud.utils.EasyCache
import dev.redicloud.utils.SingleCache
import dev.redicloud.utils.getTextOfAPIWithFallback
import dev.redicloud.utils.gson.gson
import java.util.*
import kotlin.time.Duration.Companion.minutes

class CloudServerVersionRepository(databaseConnection: DatabaseConnection) : DatabaseBucketRepository<CloudServerVersion>(databaseConnection, "server-version"){

    companion object {
        val LOGGER = LogManager.logger(CloudServerVersionRepository::class)
        val DEFAULT_VERSIONS_CACHE = SingleCache(1.minutes) {
            val json = getTextOfAPIWithFallback("api-files/versions.json")
            val type = object : TypeToken<ArrayList<CloudServerVersion>>() {}.type
            val list: MutableList<CloudServerVersion> = gson.fromJson(json, type)
            list.toList()
        }
    }

    suspend fun getVersion(uniqueId: UUID): CloudServerVersion? {
        return getHandle(uniqueId.toString()).get()
    }

    suspend fun getVersion(name: String): CloudServerVersion? {
        return getVersions().firstOrNull { it.getDisplayName().lowercase() == name.lowercase() }
    }

    suspend fun existsVersion(uniqueId: UUID): Boolean {
        return getHandle(uniqueId.toString()).isExists
    }

    suspend fun existsVersion(name: String): Boolean {
        return getVersions().any { it.getDisplayName().lowercase() == name.lowercase() }
    }

    suspend fun updateVersion(serverVersion: CloudServerVersion): CloudServerVersion {
        getHandle(serverVersion.uniqueId.toString()).set(serverVersion)
        return serverVersion
    }

    suspend fun createVersion(serverVersion: CloudServerVersion): CloudServerVersion {
        getHandle(serverVersion.uniqueId.toString()).set(serverVersion)
        return serverVersion
    }

    suspend fun deleteVersion(uniqueId: UUID): Boolean {
        return getHandle(uniqueId.toString()).delete()
    }

    suspend fun getVersions(): List<CloudServerVersion> = getAll()

    suspend fun getDefaultVersions(): List<CloudServerVersion> = DEFAULT_VERSIONS_CACHE.get() ?: emptyList()

    suspend fun updateDefaultVersions(serverVersionTypeRepository: CloudServerVersionTypeRepository) {
        getDefaultVersions().forEach {
            if (!existsVersion(it.uniqueId)) {
                LOGGER.info("Pulled version ${toConsoleValue(it.getDisplayName())} from web!")
                createVersion(it)
                return@forEach
            }
            val version = getVersion(it.uniqueId)!!
            if (version != it) {
                LOGGER.info("Pulled version ${toConsoleValue(it.getDisplayName())} from web!")
                updateVersion(it)
                if (it.typeId == null) return@forEach
                val type = serverVersionTypeRepository.getType(it.typeId!!) ?: return@forEach
                val handler = IServerVersionHandler.getHandler(type)
                if (
                    version.used &&
                    (
                        version.customDownloadUrl != it.customDownloadUrl ||
                        handler.isUpdateAvailable(it) ||
                        version.defaultFiles != it.defaultFiles
                    )
                ) {
                    handler.update(it, type)
                }
            }
        }
    }

}