package dev.redicloud.updater

import com.google.gson.reflect.TypeToken
import dev.redicloud.api.commands.ICommandManager
import dev.redicloud.logging.LogManager
import dev.redicloud.updater.suggest.BranchSuggester
import dev.redicloud.updater.suggest.BuildsSuggester
import dev.redicloud.utils.*
import dev.redicloud.utils.gson.gson
import khttp.get
import java.io.File
import java.util.*
import java.util.jar.JarFile

object Updater {

    val versionInfoFile: File = File(".update-info")
    var updateToVersion: File? = null

    suspend fun check() {
        if (versionInfoFile.exists()) {
            val info = gson.fromJson(versionInfoFile.readText(charset("UTF-8")), UpdateInfo::class.java)
            mainFolderJars().map { it to getJarProperties(it) }.filter { it.second.isNotEmpty() }.filterNot {
                it.second["branch"] == BRANCH && it.second["build"] == BUILD && it.second["version"] == CLOUD_VERSION
            }.map { it.first }.forEach {
                it.delete()
            }
            versionInfoFile.delete()
        }
        val updateInfo = updateAvailable()
        if (updateInfo.first && updateInfo.second != null) {
            LogManager.rootLogger().info("An update is available: ${updateInfo.second!!.branch}#${updateInfo.second!!.build}")
            LogManager.rootLogger().info("You can download the update with the command: version download $BRANCH ${updateInfo.second!!.build}")
            LogManager.rootLogger().info("And switch the update with the command: version switch $BRANCH ${updateInfo.second!!.build}")
        } else {
            LogManager.rootLogger().info("You are running the latest version!")
        }
    }

    fun registerSuggesters(commandManager: ICommandManager<*>) {
        commandManager.registerSuggesters(BranchSuggester(), BuildsSuggester())
    }

    fun download(branch: String, build: Int): File {
        val response = get(getRootAPIUrl() + "/files/$branch/$build/redicloud.zip")
        if (response.statusCode != 200) {
            throw IllegalStateException("Failed to download the latest build")
        }
        val versionsFolder = File("versions")
        if (!versionsFolder.exists()) {
            versionsFolder.mkdir()
        }
        val file = File("versions/redicloud-$branch#$build.zip")
        file.writeBytes(response.content)
        return file
    }

    fun switchVersion(branch: String, build: Int) {
        val versionsFolder = File("versions")
        if (!versionsFolder.exists()) {
            throw IllegalStateException("Version is not located in the versions folder")
        }
        val file = File("versions/redicloud-$branch#$build.zip")
        if (file.extension != "zip") {
            throw IllegalArgumentException("File must be a zip file")
        }
        unzipFile(file.absolutePath, File(".").absolutePath)
        var version: String = "unknown"
        updateToVersion = mainFolderJars().map { it to getJarProperties(it) }.filter {
            it.second["branch"] == branch && it.second["build"] == build.toString()
        }.map {
            version = it.second["version"] ?: "unknown"
            it.first
        }.firstOrNull() ?: throw IllegalStateException("Failed to find the version in the main folder")
        if (versionInfoFile.exists()) {
            versionInfoFile.delete()
        }
        versionInfoFile.createNewFile()
        versionInfoFile.writeText(gson.toJson(UpdateInfo(version, build.toString(), branch, BRANCH, BUILD, CLOUD_VERSION)))
}

    private fun getJarProperties(file: File): Map<String, String> {
        if (!file.exists() || file.extension != "jar") {
            return emptyMap()
        }
        val jarFile = JarFile(file)
        val properties = jarFile.getJarEntry("redicloud-version.properties")?.let {
            jarFile.getInputStream(it).use { stream ->
                val p = Properties()
                p.load(stream)
                p
            }
        } ?: throw IllegalStateException("redicloud-version.properties not found in jar file")
        return properties.map { it.key.toString() to it.value.toString() }.toMap()
    }

    private fun mainFolderJars(): List<File> {
        val mainFolder = File(".")
        return mainFolder.listFiles()?.filter { it.extension == "jar" } ?: emptyList()
    }

    fun localInstalledVersions(): Map<String, List<Int>> {
        val versionsFolder = File("versions")
        if (!versionsFolder.exists()) {
            return emptyMap()
        }
        val result = mutableMapOf<String, MutableList<Int>>()
        versionsFolder.listFiles()!!.filter { it.extension == "zip" }
            .map { it.nameWithoutExtension.replace("redicloud-", "") }
            .map { it.split("#") }
            .filter { it.size == 2 }
            .forEach {
                val branch = it[0]
                val build = it[1].toInt()
                if (result.containsKey(branch)) {
                    result[branch]!!.add(build)
                } else {
                    result[branch] = mutableListOf(build)
                }
            }
        return result
    }

    suspend fun updateAvailable(): Pair<Boolean, BuildInfo?> {
        if (BUILD == "local" || BRANCH == "local") return false to null
        val builds = getBuilds(BRANCH).filter { it.stored }
        val latestBuild = builds.filter { it.stored }.filter { it.branch == BRANCH }.maxByOrNull { it.build }
        if (latestBuild == null) {
            LogManager.rootLogger().warning("Failed to get the latest build for branch $BRANCH")
            return false to null
        }
        val updateAvailable = latestBuild.build > (BUILD.toIntOrNull() ?: -1)
        return updateAvailable to latestBuild
    }

    suspend fun getBuilds(branch: String?): List<BuildInfo> {
        if (branch == null) return emptyList()
        val response = get(getRootAPIUrl() + "/builds/?branch=$branch/")
        if (response.statusCode != 200) return emptyList()
        val type = object : TypeToken<ArrayList<BuildInfo>>() {}.type
        return gson.fromJson(response.text, type)
    }

    suspend fun getBranches(): List<String> {
        val response = get(getRootAPIUrl() + "/builds/")
        if (response.statusCode != 200) return emptyList()
        val info = gson.fromJson(response.text, BranchList::class.java)
        return info.branches
    }

}