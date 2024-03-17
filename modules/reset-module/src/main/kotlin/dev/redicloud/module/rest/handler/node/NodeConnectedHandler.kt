package dev.redicloud.module.rest.handler.node

import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.module.rest.RestConfiguration
import dev.redicloud.module.rest.RestHandler
import io.javalin.http.Context

class NodeConnectedHandler(
    restConfiguration: RestConfiguration,
    private val nodeRepository: ICloudNodeRepository
) : RestHandler(restConfiguration, "/node/connected") {

    override suspend fun handleRequest(ctx: Context) {
        nodeRepository.getConnectedNodes().map { it.serviceId }.let { ctx.json(it) }
    }

}