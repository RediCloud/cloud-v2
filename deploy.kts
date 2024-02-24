import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

val version = "2.1.4-SNAPSHOT"
val build = System.getenv("BUILD_NUMBER") ?: "local"
val git = System.getenv("BUILD_VCS_NUMBER") ?: "unknown"
val branch = System.getenv("BRANCH")?.split("/")?.last() ?: "local"

File("start-scripts").listFiles()?.filter { it.extension == "sh" || it.extension == "bat" }?.forEach {
    val lines = it.readLines()
        .map { line ->
            line.replace("%version%", version)
                .replace("%branch%", branch)
                .replace("%build%", build)
        }
    it.writeText(lines.joinToString("\n"))
}

val outPutDirs = mutableListOf(
    File("services/node-service/src/main/resources"),
)
File("connectors").listFiles()?.forEach {
    if (it.isFile) return@forEach
    if (it.name == "build" || it.name == "src") return@forEach
    outPutDirs.add(File("connectors/${it.name}/src/main/resources"))
}
outPutDirs.forEach {
    if (!it.exists()) it.mkdirs()
}
val props = createVersionProps()

outPutDirs.forEach {
    val targetFile = File(it, "redicloud-version.properties")
    if (targetFile.exists()) targetFile.delete()
    targetFile.createNewFile()
    targetFile.writeBytes(props.readBytes())
}

fun createVersionProps(): File {
    val props = File("src/main/resources/redicloud-version.properties")
    if (!props.parentFile.exists()) props.parentFile.mkdirs()
    if (props.exists()) props.delete()
    props.createNewFile()
    val writer = FileWriter(props)

    writer.write(
        "version=$version\n"
                + "build=$build\n"
                + "git=$git\n"
                + "branch=$branch"
    )
    writer.close()
    return props
}