package dev.redicloud.repository.java.version

import com.google.gson.reflect.TypeToken
import dev.redicloud.api.java.ICloudJavaVersionRepository
import dev.redicloud.api.java.ICloudJavaVersion
import dev.redicloud.api.java.ICloudJavaVersionInfo
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.utils.*
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.gson.gsonInterfaceFactory
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.utils.gson.fromJsonToList
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.minutes

class JavaVersionRepository(
    val serviceId: ServiceId,
    databaseConnection: DatabaseConnection,
    packetManager: PacketManager
) : CachedDatabaseBucketRepository<ICloudJavaVersion, CloudJavaVersion>(
    databaseConnection,
    "java-version",
    ICloudJavaVersion::class,
    CloudJavaVersion::class,
    5.minutes,
    packetManager,
    ServiceType.NODE,
), ICloudJavaVersionRepository {

    companion object {
        val ONLINE_VERSION_CACHE = EasyCache<List<CloudJavaVersion>, Unit>(1.minutes) {
            val json = getTextOfAPIWithFallback("api-files/java-versions.json")
            gson.fromJsonToList<CloudJavaVersion>(json)
        }
    }

    init {
        gsonInterfaceFactory.register(ICloudJavaVersionInfo::class, JavaVersionInfo::class)
    }

    override suspend fun getVersion(uniqueId: UUID): CloudJavaVersion? = get(uniqueId.toString())

    override suspend fun existsVersion(name: String): Boolean =
        getVersions().any { it.name.lowercase() == name.lowercase() }

    override suspend fun existsVersion(uniqueId: UUID): Boolean = exists(uniqueId.toString())

    override suspend fun getVersions(): List<CloudJavaVersion> = getAll()

    override suspend fun updateVersion(javaVersion: ICloudJavaVersion) =
        set(javaVersion.uniqueId.toString(), javaVersion)

    override suspend fun deleteVersion(uniqueId: UUID): Boolean {
        return delete(uniqueId.toString())
    }

    override suspend fun deleteVersion(javaVersion: ICloudJavaVersion): Boolean {
        return deleteVersion(javaVersion.uniqueId)
    }

    override suspend fun createVersion(javaVersion: ICloudJavaVersion): CloudJavaVersion {
        return set(javaVersion.uniqueId.toString(), javaVersion)
    }

    override suspend fun getVersion(name: String) =
        getVersions().firstOrNull { it.name.lowercase() == name.lowercase() }

    override suspend fun getOnlineVersions(): List<CloudJavaVersion> =
        ONLINE_VERSION_CACHE.get()?.toList() ?: emptyList()

    override suspend fun detectInstalledVersions(): List<CloudJavaVersion> {
        val created = getVersions()
        val versions = locateAllJavaVersions().filter {
            when (getOperatingSystemType()) {
                OSType.WINDOWS -> File(it, "bin/java.exe")
                else -> File(it, "bin/java")
            }.exists()
        }
        return versions.map { file ->
            created.forEach { javaVersion ->
                val byId = file.name.split("-").any { it.lowercase() == javaVersion.id.toString() }
                val byName = file.name.lowercase() == javaVersion.name.lowercase()
                if (byId || byName) return@map javaVersion
            }
            val javaInfo = getVersionInfo(file.absolutePath)
            return@map CloudJavaVersion(
                name = file.name,
                id = javaInfo?.versionId ?: -1,
                located = mutableMapOf(
                    serviceId.id to file.absolutePath
                ),
                info = javaInfo
            )
        }
    }

}