package dev.redicloud.api.version

import dev.redicloud.api.utils.ProcessConfiguration
import java.io.File
import java.net.URL
import java.util.*

interface ICloudServerVersionType : ProcessConfiguration {

    val uniqueId: UUID
    var name: String
    var versionHandlerName: String
    var proxy: Boolean
    val defaultType: Boolean
    var connectorPluginName: String
    var connectorDownloadUrl: String?
    var connectorFolder: String
    var libPattern: String?

    fun getParsedConnectorFile(nodeFolder: Boolean): File

    fun getParsedConnectorURL(): URL

    fun isUnknown(): Boolean

}