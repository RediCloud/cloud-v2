package dev.redicloud.testing.executables

import dev.redicloud.api.utils.TEMPLATE_FOLDER
import dev.redicloud.api.utils.toUniversalPath
import dev.redicloud.testing.RediCloudCluster
import dev.redicloud.testing.RediCloudNode
import dev.redicloud.testing.utils.FileSelect
import dev.redicloud.testing.utils.FileSelectStrategy
import dev.redicloud.testing.utils.ProjectFileSelect
import java.io.File

data class FileTemplate(
    var prefix: String,
    var name: String,
    private val inherited: MutableList<String> = mutableListOf(),
    private val localFiles: MutableMap<File, String> = mutableMapOf()
) : ICloudExecutable {

    val displayName: String
        get() = "$prefix-$name"
    val path: String
        get() = "${toUniversalPath(TEMPLATE_FOLDER.getFile())}/$prefix/$name"

    fun inherit(name: String) {
        inherited.add(name)
    }

    fun file(block: FileSelect.() -> Unit) {
        val select = FileSelect(File("")).apply(block)
        localFiles[select.file] = select.targetDirectory
    }

    override fun apply(cluster: RediCloudCluster) {
        val node = cluster.nodes.first()
        fun execute(command: String) {
            node.execute("ft $command")
            Thread.sleep(RediCloudNode.CONSOLE_COMMAND_DELAY.inWholeMilliseconds)
        }

        fun executeEdit(command: String) {
            node.execute("ft edit $name $command")
            Thread.sleep(RediCloudNode.CONSOLE_COMMAND_DELAY.inWholeMilliseconds)
        }
        execute("create $name $prefix")
        inherited.forEach { executeEdit("inherit add $it") }
    }

    override fun preApply(cluster: RediCloudCluster) {
        cluster.nodes.forEach { node ->
            val templates = TEMPLATE_FOLDER.getFile(node.localWorkingDirectory)
            localFiles.filter { it.key.exists() }.forEach { entry ->
                val file = entry.key
                val suffix = entry.value
                val targetDirectory = File(templates, "$prefix/$name/$suffix")
                targetDirectory.mkdirs()
                file.copyRecursively(File(targetDirectory, file.name), true)
            }
        }
    }

    fun gradleBuildFile(block: ProjectFileSelect.() -> Unit) {
        val select = ProjectFileSelect("", null, "", FileSelectStrategy.LATEST_MODIFIED, false).apply(block)
        localFiles[select.file] = select.targetDirectory
    }

}