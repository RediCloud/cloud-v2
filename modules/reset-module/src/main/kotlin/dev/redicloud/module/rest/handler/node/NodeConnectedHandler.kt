package dev.redicloud.module.rest.handler.node

import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.module.rest.RestHandler
import io.javalin.http.Context

class NodeConnectedHandler(
    private val nodeRepository: ICloudNodeRepository
) : RestHandler("/node/connected") {

    override suspend fun handleRequest(ctx: Context) {
        nodeRepository.getConnectedNodes().map { it.serviceId }.let { ctx.json(it) }
    }

}