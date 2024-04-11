package dev.redicloud.module.papermc

import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.serverversion.VersionRepository
import dev.redicloud.utils.gson.fromJsonToList
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.printTestSetupEnd
import dev.redicloud.utils.takeFirstLastRandom
import kotlinx.coroutines.runBlocking
import java.io.File
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
    fun testGetDownloadUrl() {
    }

    @Test
    fun testGetLatestBuild() {
    }

}