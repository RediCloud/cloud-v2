package dev.redicloud.testing.executables

import dev.redicloud.api.utils.TEMPLATE_FOLDER
import dev.redicloud.testing.RediCloudCluster
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

    fun inherit(name: String) {
        inherited.add(name)
    }

    fun file(block: FileSelect.() -> Unit) {
        val select = FileSelect(File("")).apply(block)
        localFiles[select.file] = select.targetDirectory
    }

    fun gradleBuildFile(block: ProjectFileSelect.() -> Unit) {
        val select = ProjectFileSelect("", null, "", FileSelectStrategy.LATEST_MODIFIED, false).apply(block)
        val project = File(select.projectName)
        if (!project.exists()) {
            throw IllegalArgumentException("Project $select.projectName does not exist")
        }
        val build = File(project, "build")
        if (!build.exists()) {
            throw IllegalArgumentException("Project $select.projectName does not have a build folder")
        }
        val libs = File(build, "libs")
        if (!libs.exists()) {
            throw IllegalArgumentException("Project $select.projectName does not have a libs folder")
        }
        val files =
            libs.listFiles() ?: throw IllegalArgumentException("Project $select.projectName does not have any files")
        val file = when (select.selectStrategy) {
            FileSelectStrategy.LATEST_MODIFIED -> files.filter {
                if (select.fileName != null) {
                    it.name == select.fileName
                } else {
                    true
                }
            }.filter {
                if (select.shadowJar) {
                    it.name.contains("all")
                } else {
                    true
                }
            }.maxByOrNull { it.lastModified() }
            FileSelectStrategy.OLDEST_MODIFIED -> files
                .filter {
                    if (select.fileName != null) {
                        it.name == select.fileName
                    } else {
                        true
                    }
                }.filter {
                if (select.shadowJar) {
                    it.name.contains("all")
                } else {
                    true
                }
            }.minByOrNull { it.lastModified() }
        } ?: throw IllegalArgumentException("Project $select.projectName does not have any files")
        localFiles[file] = select.targetDirectory
    }

    override fun apply(cluster: RediCloudCluster) {
        val node = cluster.nodes.first()
        fun execute(command: String) {
            node.execute("ft $command")
            Thread.sleep(100)
        }

        fun executeEdit(command: String) {
            node.execute("ft edit $name $command")
            Thread.sleep(100)
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

}