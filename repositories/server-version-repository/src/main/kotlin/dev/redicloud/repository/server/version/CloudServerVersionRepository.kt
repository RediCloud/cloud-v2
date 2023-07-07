package dev.redicloud.repository.server.version

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import java.util.*

class CloudServerVersionRepository(databaseConnection: DatabaseConnection) : DatabaseBucketRepository<CloudServerVersion>(databaseConnection, "server-version"){

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

}