import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

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
    targetFile.writeBytes(props.readBytes())
}

fun createVersionProps(): File {
    val props = File("src/main/resources/redicloud-version.properties")
    if (!props.parentFile.exists()) props.parentFile.mkdirs()
    if (props.exists()) props.delete()
    props.createNewFile()
    val writer = FileWriter(props)
    writer.write(
        "version=2.0.0-SNAPSHOT\n" +
        "buildId=${if(System.getenv("BUILD_NUMBER") == null) "local" else System.getenv("BUILD_ID")}"
    )
    writer.close()
    return props
}