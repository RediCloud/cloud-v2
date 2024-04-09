package dev.redicloud.module.rest.fetcher

import dev.redicloud.api.version.ICloudServerVersion
import dev.redicloud.api.version.ICloudServerVersionRepository
import java.util.*

class ServerVersionFetcher(
    private val serverVersionRepository: ICloudServerVersionRepository
){

    suspend fun fetchVersionById(id: String?): ICloudServerVersion? {
        return try {
            val uuid = UUID.fromString(id)
            serverVersionRepository.getVersion(uuid)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    suspend fun fetchVersionByName(name: String?): ICloudServerVersion? {
        if (name == null) {
            return null
        }
        return serverVersionRepository.getVersion(name)
    }

}