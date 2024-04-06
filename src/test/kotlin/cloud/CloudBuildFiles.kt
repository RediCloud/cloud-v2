package cloud

import java.io.File

val NODE_JAR = CloudBuildFile("services/node-service")
val BUKKIT_CONNECTOR_JAR = CloudBuildFile("connectors/bukkit-connector")
val BUNGEE_CONNECTOR_JAR = CloudBuildFile("connectors/bungeecord-connector")
val VELOCITY_CONNECTOR_JAR = CloudBuildFile("connectors/velocity-connector")
val MINESTOM_CONNECTOR_JAR = CloudBuildFile("connectors/minestom-connector")
val PAPER_MC_MODULE_JAR = CloudBuildFile("modules/papermc-updater")
val START_BAT = CloudBuildFile("start-scripts", "start-scripts/start_test.bat")
val START_SH = CloudBuildFile("start-scripts", "start-scripts/start_test.sh")

class CloudBuildFile(
    val project: String,
    val fullPath: String? = null
) {

    fun getFile(version: String? = null): File? {
            if (fullPath != null) {
                return File(fullPath)
            }
            val project = File(project)
            if (!project.exists()) {
                return null
            }
            val buildsFolder = File(project, "build/libs")
            if (!buildsFolder.exists()) {
                return null
            }
            val builds = buildsFolder.listFiles() ?: return null
            if (builds.size == 1) {
                return builds.first()
            }
            val matchedVersion = builds.filter { it.name.contains(version ?: "") }
            if (matchedVersion.isNotEmpty() && version != null) {
                return matchedVersion.first()
            }
            return builds.maxByOrNull { it.lastModified() }
        }

}