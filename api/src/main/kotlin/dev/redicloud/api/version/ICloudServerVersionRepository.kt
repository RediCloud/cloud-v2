package dev.redicloud.api.version

import java.util.UUID

interface ICloudServerVersionRepository {

    suspend fun getVersion(uniqueId: UUID): ICloudServerVersion?

    suspend fun getVersion(name: String): ICloudServerVersion?

    suspend fun existsVersion(uniqueId: UUID): Boolean

    suspend fun existsVersion(name: String): Boolean

    suspend fun deleteVersion(uniqueId: UUID): Boolean

    suspend fun updateVersion(version: ICloudServerVersion): ICloudServerVersion

    suspend fun createVersion(version: ICloudServerVersion): ICloudServerVersion

    suspend fun getVersions(): List<ICloudServerVersion>

    suspend fun getOnlineVersions(): List<ICloudServerVersion>

    suspend fun pullOnlineVersions(
        serverVersionTypeRepository: ICloudServerVersionTypeRepository,
        silent: Boolean = false
    )

}