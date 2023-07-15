package dev.redicloud.repository.java.version

import com.google.gson.reflect.TypeToken
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.utils.*
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.minutes

class JavaVersionRepository(
    val serviceId: ServiceId,
    databaseConnection: DatabaseConnection,
    packetManager: PacketManager
) : CachedDatabaseBucketRepository<JavaVersion>(
    databaseConnection,
    "java-version",
    null,
    JavaVersion::class,
    5.minutes,
    packetManager,
    ServiceType.NODE
) {

    companion object {
        val ONLINE_VERSION_CACHE = EasyCache<List<JavaVersion>, Unit> (1.minutes) {
            val json = getTextOfAPIWithFallback("api-files/java-versions.json")
            val type = object : TypeToken<ArrayList<JavaVersion>>() {}.type
            gson.fromJson<List<JavaVersion>?>(json, type)
        }
    }

    suspend fun getVersion(uniqueId: UUID): JavaVersion? = get(uniqueId.toString())

    suspend fun existsVersion(name: String): Boolean = getVersion(name) != null

    suspend fun existsVersion(uniqueId: UUID): Boolean = getHandle(uniqueId.toString()).isExists

    suspend fun getVersions(): List<JavaVersion> = getAll()

    suspend fun updateVersion(javaVersion: JavaVersion) = set(javaVersion.uniqueId.toString(), javaVersion)

    suspend fun deleteVersion(uniqueId: UUID) = delete(uniqueId.toString())

    suspend fun deleteVersion(javaVersion: JavaVersion) = deleteVersion(javaVersion.uniqueId)

    suspend fun createVersion(javaVersion: JavaVersion) = set(javaVersion.uniqueId.toString(), javaVersion)

    suspend fun getVersion(name: String) = getVersions().firstOrNull { it.name.lowercase() == name.lowercase() }

    suspend fun getOnlineVersions(): List<JavaVersion> = ONLINE_VERSION_CACHE.get()?.toList() ?: emptyList()

    suspend fun detectInstalledVersions(): List<JavaVersion> {
        val created = getVersions()
        val versions = locateAllJavaVersions().filter {
            when (OSType.WINDOWS) {
                getOperatingSystemType() -> File(it, "bin/java.exe")
                else -> File(it, "bin/java")
            }.exists()
        }
        return versions.map { file ->
            created.forEach { javaVersion ->
                val byId = file.name.split("-").any { it.lowercase() == javaVersion.id.toString() }
                val byName = file.name.lowercase() == javaVersion.name.lowercase()
                if (byId || byName) return@map javaVersion
            }
            return@map JavaVersion(
                name = file.name,
                id = detectJavaId(file.name),
                located = mutableMapOf(
                    serviceId.id to file.absolutePath
                ),
                info = parseVersionInfo(file.name)
            )
        }
    }

    private fun detectJavaId(fileName: String): Int {
        return parseVersionInfo(fileName)?.toVersionId() ?: -1
    }
}