package dev.redicloud.repository.server.version

import dev.redicloud.repository.server.version.utils.CloudServerVersionType
import dev.redicloud.repository.server.version.utils.ServerVersion

class CloudServerVersion(
    val type: CloudServerVersionType,
    val name: String,
    val customDownloadUrl: String?,
    val libPattern: String?,
    var buildId: String?,
    val version: ServerVersion
)