import org.gradle.api.Project
import java.io.File

object Builds {
    fun getOutputFileName(project: Project): String {
        return "redicloud-${project.name}-${if (project.version == "unspecified") project.parent?.version ?: "unknown" else project.version}.jar"
    }
    fun getTestDirPath(project: Project, nodeName: String): File {
        return File("${project.rootDir}/test/", nodeName)
    }

    val testNodes = 2
}