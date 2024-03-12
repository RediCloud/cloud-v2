package dev.redicloud.module.rest.handler.node

import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.module.rest.RestHandler
import io.javalin.http.Context

class NodeRegisteredHandler(
    private val nodeRepository: ICloudNodeRepository
) : RestHandler("/node/registered") {

    override suspend fun handleRequest(ctx: Context) {
        nodeRepository.getRegisteredNodes().map { it.serviceId }.let { ctx.json(it) }
    }

}