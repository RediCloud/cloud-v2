package dev.redicloud.module.papermc


import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dev.redicloud.api.version.ICloudServerVersionType
import dev.redicloud.api.version.IServerVersion
import dev.redicloud.api.version.IVersionRepository
import dev.redicloud.utils.httpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*

class PaperMcApiRequester(
    private val versionRepository: IVersionRepository
) {

    companion object {
        val BASE_URL = "https://api.papermc.io/v2"
        val gson = Gson()
        suspend inline fun <reified T> request(apiUrl: String): Response<T> {
            val response = httpClient.get { url("$BASE_URL$apiUrl") }
            val json = response.bodyAsText()
            return Response(json, gson.fromJson(json, T::class.java), response.status.value)
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
        val responseVersions = request<VersionsResponse>(url).responseObject
        val versions = responseVersions?.versions?.mapNotNull { v ->
            val r = versionRepository.parse(v)
            if (r == null) {
                println("Unknown version: $v in [${versionRepository.versions().joinToString(", ") { it.name }}]")
            }else {
                println("Parsed version: ${r.name} for $v")
            }
            r
        }?.toList() ?: emptyList()
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
    @SerializedName("project_id")
    val projectId: String,
    @SerializedName("project_name")
    val projectName: String,
    val version: String,
    val builds: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BuildsResponse

        if (projectId != other.projectId) return false
        if (projectName != other.projectName) return false
        if (version != other.version) return false
        if (!builds.contentEquals(other.builds)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = projectId.hashCode()
        result = 31 * result + projectName.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + builds.contentHashCode()
        return result
    }
}

data class VersionsResponse(
    @SerializedName("project_id")
    val projectId: String,
    @SerializedName("project_name")
    val projectName: String,
    @SerializedName("version_groups")
    val versionGroups: Array<String>,
    val versions: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VersionsResponse

        if (projectId != other.projectId) return false
        if (projectName != other.projectName) return false
        if (!versionGroups.contentEquals(other.versionGroups)) return false
        if (!versions.contentEquals(other.versions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = projectId.hashCode()
        result = 31 * result + projectName.hashCode()
        result = 31 * result + versionGroups.contentHashCode()
        result = 31 * result + versions.contentHashCode()
        return result
    }
}
