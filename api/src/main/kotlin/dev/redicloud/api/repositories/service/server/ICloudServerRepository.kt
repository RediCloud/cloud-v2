package dev.redicloud.api.repositories.service.server

import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType

interface ICloudServerRepository {

    suspend fun <T : ICloudServer> existsServer(serviceId: ServiceId): Boolean

    suspend fun <T : ICloudServer> getServer(serviceId: ServiceId): T?

    suspend fun <T : ICloudServer> getServer(name: String, type: ServiceType): T?

    suspend fun getMinecraftServer(serviceId: ServiceId): ICloudMinecraftServer?

    suspend fun getProxyServer(serviceId: ServiceId): ICloudProxyServer?

    suspend fun <T : ICloudServer> updateServer(cloudServer: T): T

    suspend fun getConnectedServers(): List<ICloudServer>

    suspend fun <T : ICloudServer> getConnectedServers(type: ServiceType): List<T>

    suspend fun getRegisteredServers(): List<ICloudServer>

    suspend fun <T : ICloudServer> getRegisteredServers(type: ServiceType): List<T>

    suspend fun getFallback(vararg currentServerIds: ServiceId?): ICloudMinecraftServer?

}