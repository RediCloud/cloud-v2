package dev.redicloud.testing.config

import dev.redicloud.testing.RediCloudCluster
import dev.redicloud.testing.executables.ConfigurationTemplate
import dev.redicloud.testing.executables.FileTemplate
import dev.redicloud.testing.executables.ICloudExecutable
import dev.redicloud.testing.executables.ServerVersion
import dev.redicloud.testing.pre.PreJavaVersion
import dev.redicloud.testing.pre.PreServerVersion
import dev.redicloud.testing.utils.VersionInfo
import dev.redicloud.utils.OSType
import dev.redicloud.utils.getOperatingSystemType
import java.util.*

data class ClusterConfiguration(
    var name: String = UUID.randomUUID().toString().substring(0, 4),
    var exposeRedis: Boolean = false,
    var attachWithWindowsTerminal: Boolean = getOperatingSystemType() == OSType.WINDOWS,
    var version: VersionInfo = VersionInfo.LATEST_STABLE,
    private val fileTemplates: MutableList<FileTemplate> = mutableListOf(),
    private val configurationTemplates: MutableList<ConfigurationTemplate> = mutableListOf(),
    private val serverVersions: MutableList<ServerVersion> = mutableListOf(),
    var cache: CacheConfig = CacheConfig(),
    internal val nodeConfigs: MutableList<NodeConfig> = mutableListOf(),
    internal val shortcuts: MutableMap<String, (RediCloudCluster) -> Unit> = mutableMapOf()
) : ICloudExecutable {

    override fun preApply(cluster: RediCloudCluster) {
        fileTemplates.forEach { it.preApply(cluster) }
        serverVersions.forEach { it.preApply(cluster) }
        configurationTemplates.forEach { it.preApply(cluster) }
    }

    override fun apply(cluster: RediCloudCluster) {
        fileTemplates.forEach { it.apply(cluster) }
        serverVersions.forEach { it.apply(cluster) }
        configurationTemplates.forEach { it.apply(cluster) }
    }

    fun version(block: VersionInfo.() -> Unit) {
        version.apply(block)
    }

    fun cache(block: CacheConfig.() -> Unit) {
        cache.apply(block)
    }

    fun node(block: NodeConfig.() -> Unit): String {
        val node = NodeConfig("node-${nodeConfigs.size+1}").apply(block)
        nodeConfigs.add(node)
        return node.name
    }

    fun fileTemplate(block: FileTemplate.() -> Unit): FileTemplate {
        val template = FileTemplate(UUID.randomUUID().toString(), UUID.randomUUID().toString()).apply(block)
        fileTemplates.add(template)
        return template
    }

    fun configureServerVersion(block: ServerVersion.() -> Unit) {
        val version = ServerVersion(PreServerVersion.PAPER_LATEST, PreJavaVersion.JAVA_21)
        version.apply(block)
        serverVersions.add(version)
    }

    fun configurationTemplate(block: ConfigurationTemplate.() -> Unit) {
        val template = ConfigurationTemplate(UUID.randomUUID().toString(), PreServerVersion.PAPER_LATEST).apply(block)
        configurationTemplates.add(template)
    }

    fun shortcut(name: String, block: (RediCloudCluster) -> Unit) {
        shortcuts[name] = block
    }

}