package dev.redicloud.module.rest.parser

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.service.server.ICloudMinecraftServer
import dev.redicloud.api.service.server.ICloudProxyServer
import dev.redicloud.api.service.server.ICloudServerRepository
import java.util.*

class ServerRestParser(
    private val serverRepository: ICloudServerRepository
) {

    suspend fun parseIdToMinecraftServer(handler: () -> String?): ICloudMinecraftServer? {
        val id = handler() ?: return null
        return try {
            val uuid = UUID.fromString(id)
            serverRepository.getMinecraftServer(ServiceId(uuid, ServiceType.MINECRAFT_SERVER))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    suspend fun parseIdToProxyServer(handler: () -> String?): ICloudProxyServer? {
        val id = handler() ?: return null
        return try {
            val uuid = UUID.fromString(id)
            serverRepository.getProxyServer(ServiceId(uuid, ServiceType.PROXY_SERVER))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

}