package dev.redicloud.repository.server.version

import dev.redicloud.repository.server.version.utils.ServerVersion
import java.util.UUID

class CloudServerVersion(
    val uniqueId: UUID,
    var typeId: UUID?,
    var projectName: String,
    var customDownloadUrl: String?,
    var libPattern: String?,
    var buildId: String?,
    var version: ServerVersion
) {
    fun getDisplayName(): String {
        return "${projectName}_${version.name}"
    }

}