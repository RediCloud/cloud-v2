package dev.redicloud.repository.server.version

import com.google.gson.reflect.TypeToken
import dev.redicloud.api.version.*
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.repository.server.version.serverversion.ServerVersion
import dev.redicloud.utils.SingleCache
import dev.redicloud.utils.getTextOfAPIWithFallback
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.gson.gsonInterfaceFactory
import dev.redicloud.api.service.ServiceType
import java.util.*
import kotlin.time.Duration.Companion.minutes

class CloudServerVersionRepository(
    databaseConnection: DatabaseConnection,
    packetManager: PacketManager
) : CachedDatabaseBucketRepository<ICloudServerVersion, CloudServerVersion>(
    databaseConnection,
    "server-version",
    null,
    ICloudServerVersion::class,
    CloudServerVersion::class,
    5.minutes,
    packetManager,
    ServiceType.NODE
), ICloudServerVersionRepository {

    companion object {
        val LOGGER = LogManager.logger(CloudServerVersionRepository::class)
        val DEFAULT_VERSIONS_CACHE = SingleCache(1.minutes) {
            gsonInterfaceFactory.register(IServerVersion::class, ServerVersion::class)
            val json = getTextOfAPIWithFallback("api-files/versions.json")
            val type = object : TypeToken<ArrayList<CloudServerVersion>>() {}.type
            val list: MutableList<CloudServerVersion> = gson.fromJson(json, type)
            list.toList()
        }
    }

    override suspend fun getVersion(uniqueId: UUID): CloudServerVersion? {
        return get(uniqueId.toString())
    }

    override suspend fun getVersion(name: String): CloudServerVersion? {
        return getVersions().firstOrNull { it.displayName.lowercase() == name.lowercase() }
    }

    override suspend fun existsVersion(uniqueId: UUID): Boolean {
        return exists(uniqueId.toString())
    }

    override suspend fun existsVersion(name: String): Boolean {
        return getVersions().any { it.displayName.lowercase() == name.lowercase() }
    }

    override suspend fun updateVersion(version: ICloudServerVersion): CloudServerVersion {
        return set(version.uniqueId.toString(), version)
    }

    override suspend fun createVersion(version: ICloudServerVersion): CloudServerVersion {
        return set(version.uniqueId.toString(), version)
    }

    override suspend fun deleteVersion(uniqueId: UUID): Boolean {
        return delete(uniqueId.toString())
    }

    override suspend fun getVersions(): List<CloudServerVersion> = getAll()

    override suspend fun getOnlineVersions(): List<CloudServerVersion> = DEFAULT_VERSIONS_CACHE.get() ?: emptyList()

    override suspend fun pullOnlineVersions(
        serverVersionTypeRepository: ICloudServerVersionTypeRepository,
        silent: Boolean
    ) {
        getOnlineVersions().forEach {
            if (!existsVersion(it.uniqueId)) {
                if (!silent) LOGGER.info("Pulled version ${toConsoleValue(it.displayName)} from web!")
                createVersion(it)
                return@forEach
            }
            val version = getVersion(it.uniqueId)!!
            if (!version.isSimilar(it)) {
                if (!silent) LOGGER.info("Pulled version ${toConsoleValue(it.displayName)} from web!")
                version.typeId = it.typeId
                version.customDownloadUrl = it.customDownloadUrl
                version.defaultFiles.clear()
                version.defaultFiles.putAll(it.defaultFiles)
                version.fileEdits.clear()
                version.fileEdits.putAll(it.fileEdits)
                version.libPattern = it.libPattern
                version.patch = it.patch
                version.programmParameters.clear()
                version.programmParameters.addAll(it.programmParameters)
                version.jvmArguments.clear()
                version.jvmArguments.addAll(it.jvmArguments)
                updateVersion(version)
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