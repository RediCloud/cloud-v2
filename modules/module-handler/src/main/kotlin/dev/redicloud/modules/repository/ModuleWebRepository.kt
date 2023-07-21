package dev.redicloud.modules.repository

import com.google.gson.reflect.TypeToken
import dev.redicloud.api.utils.MODULE_FOLDER
import dev.redicloud.modules.ModuleDescription
import dev.redicloud.modules.ModuleHandler
import dev.redicloud.utils.SingleCache
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.isValidUrl
import khttp.get
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.ArrayList
import kotlin.time.Duration.Companion.seconds

/**
 * Repo-Structure:
 * .redicloud | identify as repo
 * modules.json | list of modules (ids)
 * <module-id> | folder
 *    - module.json | module info
 *    - <version> | folder
 *        - <module-id>-<version>.jar | module file
 */

class ModuleWebRepository(
    val repoUrl: String,
    private val moduleHandler: ModuleHandler
) {

    init {
        val response = get("$repoUrl/.redicloud")
        if (response.statusCode != 200 && response.statusCode != 400) {
            throw IllegalStateException("Repository is down? ${response.statusCode} for ${repoUrl}")
        }
        if (response.statusCode == 400) {
            throw IllegalStateException("Repository is not marked as redicloud module repository. Create a file called '.redicloud' in the root of your repository. (url: $repoUrl)")
        }
    }

    private suspend inline fun <reified T> request(apiUrl: String): Response<T> {
        val response = get(repoUrl + apiUrl)
        if (response.statusCode != 200) {
            return Response("{}", null, response.statusCode)
        }
        val json = response.jsonObject.toString()
        return Response(json, gson.fromJson(json, T::class.java), response.statusCode)
    }

    private suspend inline fun <reified T> requestList(apiUrl: String): Response<List<T>> {
        val response = get(repoUrl.removeSuffix("/") + apiUrl)
        if (response.statusCode != 200) {
            return Response("{}", null, response.statusCode)
        }
        val type = object : TypeToken<ArrayList<T>>() {}.type
        val json = response.text
        val list = gson.fromJson<List<T>>(json, type)
        return Response(json, list, response.statusCode)
    }

    suspend fun hasModule(moduleId: String): Boolean {
        return getModuleInfo(moduleId) != null
    }

    private val moduleIdCache = SingleCache(15.seconds) {
        requestList<String>("/modules.json").responseObject ?: listOf()
    }
    suspend fun getModuleIds(): List<String> {
        return moduleIdCache.get() ?: listOf()
    }

    suspend fun getModuleInfo(moduleId: String): ModuleWebInfo? {
        return request<ModuleWebInfo>("/$moduleId/module.json").responseObject
    }

    suspend fun getModuleBytes(moduleId: String, version: String): ByteArray? {
        val response = get("$repoUrl/$moduleId/$version/$moduleId-$version.jar")
        return response.content
    }

    suspend fun getLatestVersion(moduleId: String): String? {
        val info = getModuleInfo(moduleId) ?: return null
        return info.versions.lastOrNull()
    }

    suspend fun isUpdateAvailable(moduleId: String): Boolean {
        val lastVersion = getLatestVersion(moduleId) ?: return false
        val description = moduleHandler.getModuleDescription(moduleId) ?: return false
        return lastVersion != description.version
    }

    //TODO: download console animation
    suspend fun download(moduleId: String, version: String): File {
        val localFile = File(MODULE_FOLDER.getFile(), "$moduleId-$version.jar")
        val bytes = getModuleBytes(moduleId, version) ?: throw IllegalStateException("Module not found: $moduleId-$version")
        val tmpFile = File(MODULE_FOLDER.getFile(), "$moduleId-$version.jar.download")
        tmpFile.createNewFile()
        tmpFile.writeBytes(bytes)
        if (localFile.exists()) localFile.delete()
        tmpFile.renameTo(localFile)
        moduleHandler.detectModules()
        return localFile
    }

}

data class Response<T>(val json: String, val responseObject: T?, val responseCode: Int)