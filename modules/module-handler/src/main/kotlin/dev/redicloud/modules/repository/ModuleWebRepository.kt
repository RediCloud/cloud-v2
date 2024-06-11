package dev.redicloud.modules.repository

import dev.redicloud.api.utils.MODULES_FOLDER
import dev.redicloud.modules.ModuleHandler
import dev.redicloud.utils.SingleCache
import dev.redicloud.utils.gson.fromJsonToList
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.httpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File
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
        val response = runBlocking {
            httpClient.get {
                url("$repoUrl/.redicloud")
            }
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Repository is down? ${response.status.value} for $repoUrl")
        }
    }

    private suspend inline fun <reified T> request(apiUrl: String): Response<T> {
        val response = httpClient.get {
            url("${repoUrl.removeSuffix("/")}$apiUrl")
        }
        if (!response.status.isSuccess()) {
            return Response("{}", null, response.status.value)
        }
        val json = response.bodyAsText()
        return Response(json, gson.fromJson(json, T::class.java), response.status.value)
    }

    private suspend inline fun <reified T> requestList(apiUrl: String): Response<List<T>> {
        val response = httpClient.get {
            url("${repoUrl.removeSuffix("/")}$apiUrl")
        }
        if (!response.status.isSuccess()) {
            return Response("{}", null, response.status.value)
        }
        val json = response.bodyAsText()
        val list = gson.fromJsonToList<T>(json)
        return Response(json, list, response.status.value)
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
        val response = httpClient.get{
            url("${repoUrl.removeSuffix("/")}/$moduleId/$version/${moduleId}-$version.jar")
        }
        return response.readBytes()
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
        MODULES_FOLDER.createIfNotExists()
        val localFile = File(MODULES_FOLDER.getFile(), "$moduleId-$version.jar")
        val bytes = getModuleBytes(moduleId, version) ?: throw IllegalStateException("Module not found: $moduleId-$version")
        val tmpFile = File(MODULES_FOLDER.getFile(), "$moduleId-$version.jar.download")
        tmpFile.createNewFile()
        tmpFile.writeBytes(bytes)
        if (localFile.exists()) localFile.delete()
        tmpFile.renameTo(localFile)
        moduleHandler.detectModules()
        return localFile
    }

}

data class Response<T>(val json: String, val responseObject: T?, val responseCode: Int)