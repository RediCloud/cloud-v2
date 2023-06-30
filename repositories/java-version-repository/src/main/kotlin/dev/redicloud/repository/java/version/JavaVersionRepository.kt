package dev.redicloud.repository.java.version

import com.google.gson.reflect.TypeToken
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.*
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.minutes

class JavaVersionRepository(
    val serviceId: ServiceId,
    databaseConnection: DatabaseConnection,
) : DatabaseBucketRepository<JavaVersion>(databaseConnection, "java-version") {

    companion object {
        val ONLINE_VERSION_CACHE = EasyCache<List<JavaVersion>, Unit> (1.minutes) {
            val json =
                khttp.get("${getRawUserContentUrl()}/api-files/java-versions.json").text
            val type = object : TypeToken<ArrayList<JavaVersion>>() {}.type
            prettyPrintGson.fromJson<List<JavaVersion>?>(json, type)
        }
    }

    init {
        runBlocking {
            try {
                createOnlineVersions()
            }catch (e: Exception) {
                LogManager.logger(JavaVersionRepository::class).severe("Failed to detect online versions", e)
            }
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

    private suspend fun createOnlineVersions() {
        getOnlineVersions().forEach {
            if (existsVersion(it.name)) return@forEach
            createVersion(it)
        }
    }

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
                    serviceId to file.absolutePath
                )
            )
        }
    }

    private fun detectJavaId(fileName: String): Int {
        return fileName.split("-").mapNotNull { it.toIntOrNull() }.firstOrNull() ?: -1
    }
}