package dev.redicloud.module.rest.handler.server

import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.module.rest.RestHandler
import io.javalin.http.Context

class ServerRegisteredHandler(
    private val serverRepository: ICloudServerRepository
) : RestHandler("/server/registered") {

    override suspend fun handleRequest(ctx: Context) {
        return serverRepository.getRegisteredServers().let { ctx.json(it) }
    }

}