package dev.redicloud.service.node.repository.server.version

import dev.redicloud.repository.server.version.utils.CloudServerVersionType
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.gson
import khttp.get

class PaperMcApiRequester(val type: CloudServerVersionType) {

    init {
        if (type != CloudServerVersionType.VELOCITY
            && type != CloudServerVersionType.PAPER
            && type != CloudServerVersionType.FOLIA
            && type != CloudServerVersionType.WATERFALL) {
            throw IllegalArgumentException("This server version is not available on papermc api")
        }
    }

    companion object {
        private val cache = mutableMapOf<String, Any>()
        private val baseUrl = "https://api.papermc.io/v2"
    }

    suspend fun getBuilds(minecraftVersion: ServerVersion): List<Int> {
        val url = "/projects/${type.name.lowercase()}/versions/${minecraftVersion.name}"
        if (cache.contains(url)) return cache[url] as List<Int>
        val builds = request<BuildsResponse>(url)
            .responseObject?.builds?.toList() ?: emptyList()
        cache[url] = builds
        return builds
    }

    suspend fun getVersions(): List<ServerVersion> {
        val url = "/projects/${type.name.lowercase()}"
        if (cache.contains(url)) return cache[url] as List<ServerVersion>
        val versions = request<VersionsResponse>(url).responseObject
            ?.versions?.mapNotNull { ServerVersion.parse(it) }?.toList() ?: emptyList()
        cache[url] = versions
        return versions
    }

    fun getDownloadUrl(minecraftVersion: ServerVersion, build: Int): String {
        return "$baseUrl/projects/${type.name.lowercase()}/versions/${minecraftVersion.name.lowercase()}/builds/$build/downloads/" +
                "${type.name.lowercase()}-${minecraftVersion.name.lowercase()}-$build.jar"
    }

    suspend fun getLatestBuild(minecraftVersion: ServerVersion): Int {
        return getBuilds(minecraftVersion).maxByOrNull { it } ?: -1
    }

    private suspend inline fun <reified T> request(apiUrl: String): Response<T> {
        val response = get(baseUrl + apiUrl)
        val json = response.jsonObject.toString()
        return Response(json, gson.fromJson(json, T::class.java), response.statusCode)
    }

}

data class Response<T>(val json: String, val responseObject: T?, val responseCode: Int)

data class BuildsResponse(
    val project_id: String,
    val project_name: String,
    val version: String,
    val builds: IntArray
)

data class VersionsResponse(
    val project_id: String,
    val project_name: String,
    val version_groups: Array<String>,
    val versions: Array<String>
)

