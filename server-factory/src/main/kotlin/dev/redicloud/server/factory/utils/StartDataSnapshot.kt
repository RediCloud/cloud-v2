package dev.redicloud.server.factory.utils

import dev.redicloud.api.service.ServiceId
import dev.redicloud.repository.java.version.CloudJavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.api.version.IServerVersionHandler
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.utils.EasyCache
import kotlin.time.Duration.Companion.seconds

class StartDataSnapshot private constructor(
    val configurationTemplate: ConfigurationTemplate
) {

    companion object {
        private val easyCache = EasyCache<StartDataSnapshot, ConfigurationTemplate>(3.seconds) {
            StartDataSnapshot(it!!)
        }
        suspend fun of(configurationTemplate: ConfigurationTemplate) = easyCache.get(configurationTemplate)!!
    }

    lateinit var version: CloudServerVersion
    lateinit var versionType: CloudServerVersionType
    lateinit var javaVersion: CloudJavaVersion
    lateinit var versionHandler: IServerVersionHandler
    lateinit var hostname: String
    var startResult: StartResult? = null

    suspend fun loadData(
        serverVersionRepository: CloudServerVersionRepository,
        serverVersionTypeRepository: CloudServerVersionTypeRepository,
        javaVersionRepository: JavaVersionRepository,
        nodeRepository: NodeRepository,
        hostServiceId: ServiceId
    ): StartResult? {
        if (configurationTemplate.serverVersionId == null) {
            startResult = UnknownServerVersionStartResult(null)
            return startResult
        }
        val version = serverVersionRepository.getVersion(configurationTemplate.serverVersionId!!)
        if (version == null ) {
            startResult = UnknownServerVersionStartResult(configurationTemplate.serverVersionId)
            return startResult
        }
        this.version = version

        if (version.typeId == null) {
            startResult = UnknownServerVersionTypeStartResult(null)
            return startResult
        }
        val versionType = serverVersionTypeRepository.getType(version.typeId!!)
        if (versionType == null) {
            startResult = UnknownServerVersionTypeStartResult(version.typeId)
            return startResult
        }
        if (versionType.isUnknown()) return UnknownServerVersionTypeStartResult(version.typeId)
        this.versionType = versionType

        if (version.javaVersionId == null) {
            startResult = UnknownJavaVersionStartResult(version.javaVersionId)
            return startResult
        }
        val javaVersion = javaVersionRepository.getVersion(version.javaVersionId!!)
        if (javaVersion == null) {
            startResult = UnknownJavaVersionStartResult(version.javaVersionId)
            return startResult
        }
        this.javaVersion = javaVersion

        // get the version handler and update/patch the version if needed
        val versionHandler = runCatching { IServerVersionHandler.getHandler(versionType) }.getOrNull()
        if (versionHandler == null) {
            startResult = UnknownServerVersionHandlerResult(version.typeId)
            return startResult
        }
        this.versionHandler = versionHandler
        val hostName = runCatching { nodeRepository.getNode(hostServiceId) }.getOrNull()?.currentOrLastSession()?.ipAddress
        if (hostName == null) {
            startResult = UnknownErrorStartResult(Exception("Cant find host node: ${hostServiceId.toName()}"))
            return startResult
        }
        this.hostname = hostName
        return null
    }

}

