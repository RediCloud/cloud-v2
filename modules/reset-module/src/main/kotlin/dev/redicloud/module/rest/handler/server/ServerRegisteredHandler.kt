package dev.redicloud.module.rest.handler.server

import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.module.rest.RestConfiguration
import dev.redicloud.module.rest.RestHandler
import io.javalin.http.Context

class ServerRegisteredHandler(
    restConfiguration: RestConfiguration,
    private val serverRepository: ICloudServerRepository
) : RestHandler(restConfiguration, "/server/registered") {

    override suspend fun handleRequest(ctx: Context) {
        return serverRepository.getRegisteredServers().let { ctx.json(it) }
    }

}