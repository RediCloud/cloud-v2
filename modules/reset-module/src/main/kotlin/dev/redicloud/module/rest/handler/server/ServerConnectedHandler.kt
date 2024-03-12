package dev.redicloud.module.rest.handler.server

import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.module.rest.RestHandler
import io.javalin.http.Context

class ServerConnectedHandler(
    private val serverRepository: ICloudServerRepository
) : RestHandler("/server/connected") {

    override suspend fun handleRequest(ctx: Context) {
        return serverRepository.getConnectedServers().let { ctx.json(it) }
    }

}