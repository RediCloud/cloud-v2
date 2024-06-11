package dev.redicloud.module.papermc

import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.serverversion.VersionRepository
import dev.redicloud.utils.gson.fromJsonToList
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.isValidUrl
import dev.redicloud.utils.printTestSetupEnd
import dev.redicloud.utils.takeFirstLastRandom
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.max
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class PaperMcApiRequesterTest {

    companion object {
        const val PRE_TYPES_FILE = "../../api-files/server-version-types.json"
        const val PRE_VERSIONS_FILE = "../../api-files/versions.json"
        const val REQUEST_LIMIT_PER_TEST = 3
    }

    private val requester = PaperMcApiRequester(VersionRepository)
    private val types = mutableListOf<CloudServerVersionType>()
    private val versions = mutableListOf<CloudServerVersion>()

    @BeforeTest
    fun setup() {
        val typesFile = File(PRE_TYPES_FILE)
        assertTrue(typesFile.isFile, "Version-types api-file not found (${typesFile.absolutePath})!")
        typesFile.readText().let {
            types.addAll(gson.fromJsonToList(it))
        }
        assertTrue(types.isNotEmpty(), "Version-types from api-files are empty!")
        println("Types: ${types.joinToString(", ") { it.name }}")

        val versionsFile = File(PRE_VERSIONS_FILE)
        assertTrue(versionsFile.isFile, "Versions api-file not found (${versionsFile.absolutePath})!")
        versionsFile.readText().let {
            versions.addAll(gson.fromJsonToList(it))
        }
        assertTrue(versions.isNotEmpty(), "Versions from api-files are empty!")
        println("Versions: ${versions.joinToString(", ") { it.displayName }}")

        runBlocking { VersionRepository.loadIfNotLoaded() }
        assertTrue(VersionRepository.versions().isNotEmpty(), "Versions from repository are empty!")
        printTestSetupEnd()
    }

    @Test
    fun testGetBuilds() = runBlocking {
        versions.filter { it.typeId != null }
            .map { version -> version to types.firstOrNull { it.uniqueId == version.typeId } }
            .filter { it.second != null }
            .filter { it.second!!.versionHandlerName == "papermc"}
            .filterNot { it.first.version.latest }
            .takeFirstLastRandom(REQUEST_LIMIT_PER_TEST).toList()
            .forEach { (version, type) ->
                val builds = requester.getBuilds(type!!, version.version)
                assertTrue(builds.isNotEmpty(), "No builds found for version ${version.displayName}!")
                println("Builds for ${version.displayName}: ${builds.joinToString(", ")}")
            }
    }

    @Test
    fun testGetVersions() = runBlocking {
        types.filter { it.versionHandlerName == "papermc" }
            .takeFirstLastRandom(REQUEST_LIMIT_PER_TEST).forEach { type ->
                val versions = requester.getVersions(type)
                assertTrue(versions.isNotEmpty(), "No versions found for type ${type.name}!")
                println("Versions for ${type.name}: ${versions.joinToString(", ") { it.name }}")
            }
    }

    @Test
    fun testGetDownloadUrl() = runBlocking {
        versions.filter { it.typeId != null }
            .map { version -> version to types.firstOrNull { it.uniqueId == version.typeId } }
            .filter { it.second != null }
            .filter { it.second!!.versionHandlerName == "papermc"}
            .filterNot { it.first.version.latest }
            .takeFirstLastRandom(REQUEST_LIMIT_PER_TEST).toList()
            .forEach { (version, type) ->
                val count = REQUEST_LIMIT_PER_TEST / 2
                val builds = requester.getBuilds(type!!, version.version).takeFirstLastRandom(max(1, count))
                assertTrue(builds.isNotEmpty(), "No builds found for version ${version.displayName}!")
                builds.forEach { build ->
                    val url = requester.getDownloadUrl(type, version.version, build)
                    assertTrue(url.isNotEmpty(), "No download-url found for version ${version.displayName}!")
                    println("Download-url for ${version.displayName} build $build: $url")
                    assertTrue(isValidUrl(url), "Download-url $url is not valid for version ${version.displayName}!")
                }
            }
    }

    @Test
    fun testGetLatestBuild() = runBlocking {
        val versions = versions.asSequence().filter { it.typeId != null }
            .map { version -> version to types.firstOrNull { it.uniqueId == version.typeId } }
            .filter { it.second != null }
            .filter { it.second!!.versionHandlerName == "papermc"}
            .filterNot { it.first.version.latest }.toList()
            .toSet()
            .takeFirstLastRandom(REQUEST_LIMIT_PER_TEST).toList()
        versions.forEach { (version, type) ->
            val latestBuild = requester.getLatestBuild(type!!, version.version)
            assertTrue(latestBuild >= 0, "No latest-build found for version ${version.displayName}!")
            println("Latest-build for ${version.displayName}: $latestBuild")
        }
    }

}