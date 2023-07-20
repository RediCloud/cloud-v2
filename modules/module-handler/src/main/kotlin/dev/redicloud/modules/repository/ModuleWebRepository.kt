package dev.redicloud.modules.repository

import com.google.gson.reflect.TypeToken
import dev.redicloud.api.utils.MODULE_FOLDER
import dev.redicloud.modules.ModuleDescription
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.isValidUrl
import khttp.get
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.ArrayList

/*
   Repo-Structure:
   .redicloud | identify as repo
   modules.json | list of modules (ids)
   your-module-1 | folder
       - module.json | module info
       - module.jar | module file
   your-module-1 | folder
       - module.json | module info
       - module.jar | module file
*/

class ModuleWebRepository(
    val repoUrl: String
) {

    init {
        runBlocking {
            if (!isValidUrl(repoUrl)) throw IllegalArgumentException("Invalid module-repo url: ${repoUrl}")
            val validResponse = runBlocking { request<String>("/.redicloud") }
            if (validResponse.responseCode != 200 && validResponse.responseCode != 400) throw IllegalStateException("Repository is down? ${validResponse.responseCode} for ${repoUrl}")
            if (validResponse.responseCode == 400) throw IllegalStateException("Repository is not marked as redicloud module repository. Create a file called '.redicloud' in the root of your repository. (url: $repoUrl)")
        }
    }

    private suspend inline fun <reified T> request(apiUrl: String): Response<T> {
        val response = get(repoUrl + apiUrl)
        val json = response.jsonObject.toString()
        return Response(json, gson.fromJson(json, T::class.java), response.statusCode)
    }

    private suspend inline fun <reified T> requestList(apiUrl: String): Response<List<T>> {
        val response = get(repoUrl.removeSuffix("/") + apiUrl)
        val type = object : TypeToken<ArrayList<T>>() {}.type
        val json = response.text
        val list = gson.fromJson<List<T>>(json, type)
        return Response(json, list, response.statusCode)
    }

    suspend fun hasModule(moduleId: String): Boolean {
        return getModuleInfo(moduleId) != null
    }

    suspend fun getModuleIds(): List<String> {
        return requestList<String>("modules.json").responseObject ?: listOf()
    }

    suspend fun getModuleInfo(moduleId: String): ModuleWebInfo? {
        return request<ModuleWebInfo>("/$moduleId/module.json").responseObject
    }

    suspend fun getModuleBytes(moduleId: String, version: String): ByteArray? {
        val info = getModuleInfo(moduleId) ?: return null
        val versionPath = info.versions[version] ?: return null
        val response = get("$repoUrl/${versionPath.removePrefix("/").removeSuffix("/")}/$moduleId-$version.jar")
        return response.content
    }

    suspend fun getLatestVersion(moduleId: String): String? {
        val info = getModuleInfo(moduleId) ?: return null
        return info.versions.keys.lastOrNull()
    }

    suspend fun isUpdateAvailable(description: ModuleDescription): Boolean {
        val lastVersion = getLatestVersion(description.id) ?: return false
        return lastVersion != description.version
    }

    //TODO: download console animation
    suspend fun download(moduleId: String, version: String) {
        val localFile = File(MODULE_FOLDER.getFile(), "$moduleId-$version.jar")
        val bytes = getModuleBytes(moduleId, version) ?: throw IllegalStateException("Module not found: $moduleId-$version")
        val tmpFile = File(MODULE_FOLDER.getFile(), "$moduleId-$version.jar.download")
        tmpFile.writeBytes(bytes)
        if (localFile.exists()) localFile.delete()
        tmpFile.renameTo(localFile)
    }

}

data class Response<T>(val json: String, val responseObject: T?, val responseCode: Int)