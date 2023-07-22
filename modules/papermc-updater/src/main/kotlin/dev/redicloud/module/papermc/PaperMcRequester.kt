package dev.redicloud.module.papermc


import com.google.gson.Gson
import dev.redicloud.api.version.ICloudServerVersionType
import dev.redicloud.api.version.IServerVersion
import dev.redicloud.api.version.IVersionRepository
import khttp.get

class PaperMcApiRequester(
    private val versionRepository: IVersionRepository
) {

    companion object {
        val BASE_URL = "https://api.papermc.io/v2"
        val gson = Gson()
        suspend inline fun <reified T> request(apiUrl: String): Response<T> {
            val response = get(BASE_URL + apiUrl)
            val json = response.jsonObject.toString()
            return Response(json, gson.fromJson(json, T::class.java), response.statusCode)
        }
    }

    private val cache = mutableMapOf<String, Any>()

    suspend fun getBuilds(type: ICloudServerVersionType, minecraftVersion: IServerVersion): List<Int> {
        val url = "/projects/${type.name.lowercase()}/versions/${minecraftVersion.name}"
        if (cache.contains(url)) return cache[url] as List<Int>
        val builds = request<BuildsResponse>(url)
            .responseObject?.builds?.toList() ?: emptyList()
        cache[url] = builds
        return builds
    }

    suspend fun getVersions(type: ICloudServerVersionType): List<IServerVersion> {
        val url = "/projects/${type.name.lowercase()}"
        if (cache.contains(url)) return cache[url] as List<IServerVersion>
        val versions = request<VersionsResponse>(url).responseObject
            ?.versions?.mapNotNull { versionRepository.parse(it) }?.toList() ?: emptyList() //TODO only paper able versions
        cache[url] = versions
        return versions
    }

    fun getDownloadUrl(type: ICloudServerVersionType, minecraftVersion: IServerVersion, build: Int): String {
        return "$BASE_URL/projects/${type.name.lowercase()}/versions/${minecraftVersion.name}/builds/$build/downloads/" +
                "${type.name.lowercase()}-${minecraftVersion.name}-$build.jar"
    }

    suspend fun getLatestBuild(type: ICloudServerVersionType, minecraftVersion: IServerVersion): Int {
        return getBuilds(type, minecraftVersion).maxByOrNull { it } ?: -1
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
