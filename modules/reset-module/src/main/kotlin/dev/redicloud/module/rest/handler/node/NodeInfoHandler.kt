package dev.redicloud.module.rest.handler.node

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.module.rest.RestConfiguration
import dev.redicloud.module.rest.RestHandler
import dev.redicloud.module.rest.parser.NodeRestParser
import io.javalin.http.Context
import java.util.*

class NodeInfoHandler(
    restConfiguration: RestConfiguration,
    private val nodeRestParser: NodeRestParser
) : RestHandler(restConfiguration, "/node/info/{nodeId}") {

    override suspend fun handleRequest(ctx: Context) {
        val node = nodeRestParser.parseIdToNode { ctx.pathParam("nodeId") }
        if (node == null) {
            ctx.status(404)
            ctx.json(mapOf("error" to "Node not found"))
            return
        }
        ctx.json(node)
    }

}