package dev.redicloud.api.version

import dev.redicloud.api.utils.ProcessConfiguration
import java.util.*

interface ICloudServerVersion : ProcessConfiguration {

    val uniqueId: UUID
    var typeId: UUID?
    var projectName: String
    var customDownloadUrl: String?
    var buildId: String?
    var version: IServerVersion
    var javaVersionId: UUID?
    var libPattern: String?
    var patch: Boolean
    val online: Boolean
    val used: Boolean
    val displayName: String

}