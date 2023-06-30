package dev.redicloud.repository.java.version

import com.google.gson.reflect.TypeToken
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.utils.EasyCache
import dev.redicloud.utils.getRawUserContentUrl
import dev.redicloud.utils.prettyPrintGson
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.time.Duration.Companion.minutes

class JavaVersionRepository(
    databaseConnection: DatabaseConnection,
) : DatabaseBucketRepository<JavaVersion>(databaseConnection, "java-version") {

    companion object {
        val ONLINE_VERSION_CACHE = EasyCache<List<JavaVersion>, Unit> (1.minutes) {
            val json =
                khttp.get("${getRawUserContentUrl()}/api-files/java-versions.json").text
            val type = object : TypeToken<ArrayList<JavaVersion>>() {}.type
            prettyPrintGson.fromJson(json, type)
        }
    }

    init {
        runBlocking {
            createOnlineVersions()
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

    suspend fun getOnlineVersions(): List<JavaVersion> = ONLINE_VERSION_CACHE.get() ?: emptyList()

    private suspend fun createOnlineVersions() {
        getOnlineVersions().forEach {
            if (existsVersion(it.name)) return@forEach
            createVersion(it)
        }
    }
}