package dev.redicloud.module.rest.handler.server

import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.module.rest.RestHandler
import dev.redicloud.module.rest.fetcher.ServerFetcher
import io.javalin.http.Context

class MinecraftServerInfoHandler(
    private val serverRepository: ICloudServerRepository,
    private val serverFetcher: ServerFetcher,
    config: IModuleStorage
) : RestHandler(config, "/server/minecraft/info") {

    override suspend fun handleRequest(ctx: Context) {
        if (!ctx.queryParam("id").isNullOrEmpty()) {
            val server = serverFetcher.fetchMinecraftServerById(ctx.queryParam("id")!!)
            if (server == null) {
                ctx.status(404)
                ctx.json(mapOf("error" to "Minecraft server not found"))
                return
            }
            ctx.json(server)
            return
        }
        val servers = if (ctx.queryParam("online")?.toBooleanStrictOrNull() == true) {
            serverRepository.getConnectedServers()
        } else {
            serverRepository.getRegisteredServers()
        }.filter { it.serviceId.type == ServiceType.MINECRAFT_SERVER }

        ctx.json(servers)
    }

}