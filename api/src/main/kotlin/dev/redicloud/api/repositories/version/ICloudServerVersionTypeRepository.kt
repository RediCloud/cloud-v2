package dev.redicloud.api.repositories.version

import java.util.UUID

interface ICloudServerVersionTypeRepository {

    suspend fun getType(name: String): ICloudServerVersionType?

    suspend fun getType(version: ICloudServerVersion): ICloudServerVersionType?

    suspend fun getType(uniqueId: UUID): ICloudServerVersionType?

    suspend fun existsType(name: String): Boolean

    suspend fun existsType(uniqueId: UUID): Boolean

    suspend fun deleteType(type: ICloudServerVersionType): Boolean

    suspend fun deleteType(uniqueId: UUID): Boolean

    suspend fun updateType(type: ICloudServerVersionType): ICloudServerVersionType

    suspend fun createType(type: ICloudServerVersionType): ICloudServerVersionType

    suspend fun getTypes(): List<ICloudServerVersionType>

    suspend fun getOnlineTypes(): List<ICloudServerVersionType>

    suspend fun downloadConnector(serverVersionType: ICloudServerVersionType, force: Boolean = false, lock: Boolean = true)

    suspend fun pullOnlineTypes(
        serverVersionRepository: ICloudServerVersionRepository,
        silent: Boolean = false
    )

}