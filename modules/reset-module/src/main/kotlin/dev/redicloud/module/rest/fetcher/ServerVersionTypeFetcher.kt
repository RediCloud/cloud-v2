package dev.redicloud.module.rest.fetcher

import dev.redicloud.api.version.ICloudServerVersionType
import dev.redicloud.api.version.ICloudServerVersionTypeRepository
import java.util.*

class ServerVersionTypeFetcher(
    private val serverVersionType: ICloudServerVersionTypeRepository
) {

    suspend fun fetchVersionTypeById(id: String?): ICloudServerVersionType? {
        return try {
            val uuid = UUID.fromString(id)
            serverVersionType.getType(uuid)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    suspend fun fetchVersionTypeByName(name: String?): ICloudServerVersionType? {
        if (name == null) {
            return null
        }
        return serverVersionType.getType(name)
    }

}