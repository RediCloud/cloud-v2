
import com.google.gson.GsonBuilder
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.requester.BuildsResponse
import dev.redicloud.repository.server.version.requester.PaperMcApiRequester
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.gson.fixKotlinAnnotations
import dev.redicloud.utils.isValidUrl
import java.util.*

val gson = GsonBuilder().serializeNulls().setPrettyPrinting().fixKotlinAnnotations().create()

suspend fun main() {
    ServerVersion.loadOnlineVersions()
    val list = mutableListOf<CloudServerVersion>()
    list.addAll(generateSpigotVersions())
    list.addAll(generateBungeeCord())
    list.addAll(generatePaperVersions())
    println(gson.toJson(list))
}

suspend fun generatePaperVersions(): List<CloudServerVersion> {
    val list = mutableListOf<CloudServerVersion>()
    list.addAll(generatePaperMCVersions(
        "paper",
        UUID.fromString("b692f35a-42bd-45db-aec1-db1867bc19aa"),
        true
    ))
    list.addAll(generatePaperMCVersions(
        "velocity",
        UUID.fromString("e0982b0f-f27f-4206-a373-ce12ad8ea333"),
        false
    ))
    list.addAll(generatePaperMCVersions(
        "waterfall",
        UUID.fromString("ebd2b13a-a25b-443c-876f-ab494a18354f"),
        false
    ))
    list.addAll(generatePaperMCVersions(
        "folia",
        UUID.fromString("6fa280a8-aaed-4ee7-99e4-c337b6985098"),
        true
    ))
    return list
}

suspend fun generatePaperMCVersions(typeName: String, typeId: UUID, patch: Boolean): List<CloudServerVersion> {
    ServerVersion.loadIfNotLoaded()
    val onlineVersions = mutableListOf<CloudServerVersion>()
    ServerVersion.versions().filter { !it.isUnknown() }.forEach { version ->
        val versionName = if (version.isLatest()) version.dynamicVersion().name else version.name
        val url = "/projects/${typeName.lowercase()}/versions/${versionName.lowercase()}"
        val builds = PaperMcApiRequester.request<BuildsResponse>(url)
            .responseObject?.builds?.toList() ?: emptyList()
        val buildId = builds.maxByOrNull { it } ?: -1
        if (buildId == -1) return@forEach
        val cloudVersion = CloudServerVersion(
            UUID.randomUUID(),
            typeId,
            typeName.lowercase(),
            "https://api.papermc.io/v2/projects/${typeName.lowercase()}/versions/${versionName.lowercase()}/builds/%build_id%/downloads/paper-%version_name%-%build_id%.jar",
            buildId.toString(),
            version,
            null,
            null,
            patch,
            true,
            false,
            mutableListOf(),
            mutableMapOf(),
            mutableListOf(),
            mutableMapOf(),
            mutableMapOf()
        )
        onlineVersions.add(cloudVersion)
    }
    return onlineVersions
}

suspend fun generateSpigotVersions(): List<CloudServerVersion> {
    ServerVersion.loadIfNotLoaded()
    val onlineVersions = mutableListOf<CloudServerVersion>()

    ServerVersion.versions().forEach { version ->
        val versionName = if (version.isLatest()) version.dynamicVersion().name else version.name
        val url = "https://download.getbukkit.org/spigot/spigot-${versionName.lowercase()}.jar"
        if (!isValidUrl(url)) return@forEach
        val cloudVersion = CloudServerVersion(
            UUID.randomUUID(),
            UUID.fromString("6760e0ea-455d-44bc-84c4-ec603125e61a"),
            "spigot",
            url,
            null,
            version,
            null,
            null,
            true,
            true,
            false,
            mutableListOf(),
            mutableMapOf(),
            mutableListOf(),
            mutableMapOf(),
            mutableMapOf()
        )
        onlineVersions.add(cloudVersion)
    }
    return onlineVersions
}

suspend fun generateBungeeCord(): List<CloudServerVersion> {
    ServerVersion.loadIfNotLoaded()
    val latest = ServerVersion.versions().first { it.isLatest() }
    val url = "https://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar"
    val cloudVersion = CloudServerVersion(
        UUID.randomUUID(),
        UUID.fromString("6dc3fa6b-d8ae-4e31-b40e-519b43671e5b"),
        "bungeecord",
        url,
        null,
        latest,
        null,
        null,
        false,
        true,
        false,
        mutableListOf(),
        mutableMapOf(),
        mutableListOf(),
        mutableMapOf(),
        mutableMapOf()
    )
    return listOf(cloudVersion)
}