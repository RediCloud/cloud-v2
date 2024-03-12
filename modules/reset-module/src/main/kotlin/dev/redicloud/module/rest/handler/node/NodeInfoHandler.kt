package dev.redicloud.module.rest.handler.node

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.module.rest.RestHandler
import io.javalin.http.Context
import java.util.*

class NodeInfoHandler(
    private val nodeRepository: ICloudNodeRepository
) : RestHandler("/node/info/{nodeId}") {

    override suspend fun handleRequest(ctx: Context) {
        val nodeId = runCatching { UUID.fromString(ctx.pathParam("nodeId")) }.getOrNull()
        if (nodeId == null) {
            ctx.status(400)
            ctx.json(mapOf("error" to "Invalid node id"))
            return
        }
        val node = nodeRepository.getNode(ServiceId(nodeId, ServiceType.NODE))
        if (node == null) {
            ctx.status(404)
            ctx.json(mapOf("error" to "Node not found"))
            return
        }
        ctx.json(node)
    }

}