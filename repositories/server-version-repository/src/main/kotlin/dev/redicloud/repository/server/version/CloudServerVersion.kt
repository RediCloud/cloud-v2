package dev.redicloud.repository.server.version

import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.server.version.utils.CloudServerVersionType
import dev.redicloud.repository.server.version.utils.ServerVersion
import java.util.UUID

class CloudServerVersion(
    val uniqueId: UUID,
    var type: CloudServerVersionType,
    var name: String,
    var customDownloadUrl: String?,
    var libPattern: String?,
    var buildId: String?,
    var version: ServerVersion
) {
    fun getHandle(): IServerVersionHandler {
        return IServerVersionHandler.getHandler(type)
    }
}