import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

val version = "2.3.2-SNAPSHOT"
val build = System.getenv("build_number") ?: "local"
val git = System.getenv("build_vcs_number") ?: "unknown"
val branch = System.getenv("branch")?.replace("refs/heads/", "") ?: "local"

File("start-scripts").listFiles()?.filter { it.extension == "sh" || it.extension == "bat" }?.forEach {
    val lines = it.readLines()
        .map { line ->
            line.replace("%version%", version)
                .replace("%branch%", branch.replace("/", "+"))
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
