package dev.redicloud.repository.server.version

import com.google.gson.reflect.TypeToken
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.utils.EasyCache
import dev.redicloud.utils.getRawUserContentUrl
import dev.redicloud.utils.prettyPrintGson
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class CloudServerVersionTypeRepository(
    databaseConnection: DatabaseConnection,
    serverVersionRepository: CloudServerVersionRepository
) : DatabaseBucketRepository<CloudServerVersionType>(databaseConnection, "server-version-types") {

    init {
        runBlocking { IServerVersionHandler.registerDefaultHandlers(serverVersionRepository, this@CloudServerVersionTypeRepository) }
    }

    companion object {
        val DEFAULT_TYPES_CACHE = EasyCache<List<CloudServerVersionType>, Unit> (1.minutes) {
            val json =
                khttp.get("${getRawUserContentUrl()}/api-files/server-version-types.json").text
            val type = object : TypeToken<ArrayList<CloudServerVersionType>>() {}.type
            val list: MutableList<CloudServerVersionType> = prettyPrintGson.fromJson<MutableList<CloudServerVersionType>?>(json, type)
            if (list.none { it.isUnknown() }) {
                list.add(
                    CloudServerVersionType(
                        UUID.randomUUID(),
                        "unknown",
                        "urldownloader",
                        false,
                        false,
                        mutableListOf(),
                        mutableListOf(),
                        mutableMapOf(),
                        true
                    )
                )
            }
            list.toList()
        }
    }

    init {
        runBlocking {
            createDefaultTypes()
        }
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

    private suspend fun createDefaultTypes() {
        val defaultTypes = getDefaultTypes()
        defaultTypes.forEach {
            if (existsType(it.uniqueId)) {
                updateType(it)
                return@forEach
            }
            createType(it)
        }
    }

}