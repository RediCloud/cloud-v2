package dev.redicloud.testing.config

import dev.redicloud.testing.RediCloudCluster
import dev.redicloud.testing.executables.ConfigurationTemplate
import dev.redicloud.testing.executables.FileTemplate
import dev.redicloud.testing.executables.ICloudExecutable
import dev.redicloud.testing.utils.VersionInfo
import dev.redicloud.utils.OSType
import dev.redicloud.utils.getOperatingSystemType
import java.util.*

data class ClusterConfiguration(
    var name: String = UUID.randomUUID().toString().substring(0, 4),
    var nodes: Int = 1,
    var attachWithWindowsTerminal: Boolean = getOperatingSystemType() == OSType.WINDOWS,
    var version: VersionInfo = VersionInfo.LATEST_STABLE,
    private val fileTemplates: MutableList<FileTemplate> = mutableListOf(),
    private val configurationTemplates: MutableList<ConfigurationTemplate> = mutableListOf()
) : ICloudExecutable {

    override fun preApply(cluster: RediCloudCluster) {
        fileTemplates.forEach { it.preApply(cluster) }
        configurationTemplates.forEach { it.preApply(cluster) }
    }

    override fun apply(cluster: RediCloudCluster) {
        fileTemplates.forEach { it.apply(cluster) }
        configurationTemplates.forEach { it.apply(cluster) }
    }

    fun version(block: VersionInfo.() -> Unit) {
        version.apply(block)
    }

    fun fileTemplate(block: FileTemplate.() -> Unit) {
        val template = FileTemplate("", "").apply(block)
        fileTemplates.add(template)
    }

    fun configurationTemplate(block: ConfigurationTemplate.() -> Unit) {
        val template = ConfigurationTemplate("", "").apply(block)
        configurationTemplates.add(template)
    }

}