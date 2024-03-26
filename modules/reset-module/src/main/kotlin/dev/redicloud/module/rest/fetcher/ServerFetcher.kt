package dev.redicloud.module.rest.fetcher

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.service.server.ICloudMinecraftServer
import dev.redicloud.api.service.server.ICloudProxyServer
import dev.redicloud.api.service.server.ICloudServerRepository
import java.util.*

class ServerFetcher(
    private val serverRepository: ICloudServerRepository
) {

    suspend fun fetchMinecraftServerById(id: String?): ICloudMinecraftServer? {
        return try {
            val uuid = UUID.fromString(id)
            serverRepository.getMinecraftServer(ServiceId(uuid, ServiceType.MINECRAFT_SERVER))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    suspend fun fetchProxyServerById(id: String?): ICloudProxyServer? {
        return try {
            val uuid = UUID.fromString(id)
            serverRepository.getProxyServer(ServiceId(uuid, ServiceType.PROXY_SERVER))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

}