package dev.redicloud.module.rest.handler.node

import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.module.rest.RestHandler
import dev.redicloud.module.rest.fetcher.NodeFetcher
import io.javalin.http.Context

class NodeInfoHandler(
    private val nodeRepository: ICloudNodeRepository,
    private val nodeFetcher: NodeFetcher,
    config: IModuleStorage,
) : RestHandler(config, "/node/info") {

    override suspend fun handleRequest(ctx: Context) {
        if (!ctx.queryParam("nodeId").isNullOrEmpty()) {
            val nodeId = ctx.queryParam("nodeId")!!
            val node = nodeFetcher.fetchNodeById(nodeId)
            if (node == null) {
                ctx.json(mapOf("error" to "Node not found"))
                return
            }
            ctx.json(node)
            return
        }
        if (ctx.queryParam("master")?.toBooleanStrictOrNull() == true) {
            val masterNode = nodeRepository.getMasterNode()
            if (masterNode == null) {
                ctx.json(mapOf("error" to "Master node not found"))
                return
            }
            ctx.json(masterNode)
            return
        }
        val nodes = if (ctx.queryParam("connected")?.toBooleanStrictOrNull() == true) {
            nodeRepository.getConnectedNodes()
        } else {
            nodeRepository.getRegisteredNodes()
        }
        ctx.json(nodes)
    }

}