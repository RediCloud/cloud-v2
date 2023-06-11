package dev.redicloud.repository.server.version

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository

class ServerVersionRepository(databaseConnection: DatabaseConnection) : DatabaseBucketRepository<CloudServerVersion>(databaseConnection, "server-version:"){

    suspend fun getVersion(name: String): CloudServerVersion? {
        return getHandle(name.lowercase()).get()
    }

    suspend fun existsVersion(name: String): Boolean {
        return getHandle(name.lowercase()).isExists
    }

    suspend fun updateVersion(serverVersion: CloudServerVersion, oldName: String? = null): CloudServerVersion {
        return if (oldName != null) {
            getHandle(oldName.lowercase()).set(serverVersion)
            serverVersion
        }else {
            getHandle(serverVersion.name.lowercase())
            serverVersion
        }
    }

    suspend fun createVersion(serverVersion: CloudServerVersion): CloudServerVersion {
        getHandle(serverVersion.name.lowercase()).set(serverVersion)
        return serverVersion
    }

    suspend fun getVersions(): List<CloudServerVersion> = getAll()

}