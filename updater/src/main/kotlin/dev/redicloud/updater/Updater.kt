package dev.redicloud.updater

import dev.redicloud.utils.*
import dev.redicloud.utils.gson.gson
import khttp.get
import java.io.File

object Updater {

    fun download(branch: String, build: Int): File {
        val response = get(getAPIUrl() + "/build/$branch/$build/redicloud.zip")
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

    fun activateVersion(branch: String, build: Int) {
        val versionsFolder = File("versions")
        if (!versionsFolder.exists()) {
            throw IllegalStateException("Version is not located in the versions folder")
        }
        val file = File("versions/redicloud-$branch#$build.zip")
        if (file.extension != "zip") {
            throw IllegalArgumentException("File must be a zip file")
        }
        unzipFile(file.absolutePath, File(".").absolutePath)
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

    suspend fun updateAvailable(): Pair<Boolean, Int?> {
        if (BUILD == "local" || BRANCH == "local") return false to null
        val projectInfo = getProjectInfo(BRANCH) ?: return false to null
        val updateAvailable = (projectInfo.builds.maxOrNull() ?: Int.MAX_VALUE) > (BUILD.toIntOrNull()?: -1)
        return updateAvailable to projectInfo.builds.maxOrNull()
    }

    suspend fun getProjectInfo(branch: String?): ProjectInfo? {
        if (branch == null) return null
        val response = get(getRootAPIUrl() + "/project-info/$branch/")
        if (response.statusCode != 200) return null
        val projects = gson.fromJson(response.text, ProjectInfo::class.java)
        return projects
    }

    suspend fun getBuilds(branch: String): List<Int>? {
        return getProjectInfo(branch)?.builds
    }

    suspend fun getBranches(): List<String> {
        val response = get(getRootAPIUrl() + "/project-info/list")
        if (response.statusCode != 200) return emptyList()
        val projects = gson.fromJson(response.text, Array<String>::class.java)
        return projects.toList()
    }

}