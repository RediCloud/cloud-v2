package dev.redicloud.utils.versions

import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import dev.redicloud.utils.prettyPrintGson
import org.json.JSONArray

data class JavaVersion(val version: String, val id: Int) {
    companion object {

        fun values(): List<JavaVersion> = CACHED_JAVA_VERSIONS

        fun parse(s: String): JavaVersion? {
            return CACHED_JAVA_VERSIONS.firstOrNull { it.version.lowercase() == s.lowercase() }
        }

        private val CACHED_JAVA_VERSIONS = mutableListOf<JavaVersion>()

        fun versions(): List<JavaVersion> = CACHED_JAVA_VERSIONS

        suspend fun loadOnlineVersions() {
            CACHED_JAVA_VERSIONS.clear()
            val json =
                khttp.get("https://raw.githubusercontent.com/RediCloud/cloud-v2/master/api-files/java-versions.json").text
            val type = object : TypeToken<ArrayList<JavaVersion>>() {}.type
            val list: List<JavaVersion> = prettyPrintGson.fromJson(json, type)
            CACHED_JAVA_VERSIONS.addAll(list)
        }

        suspend fun loadIfNotLoaded() {
            if (CACHED_JAVA_VERSIONS.isNotEmpty()) return
            loadOnlineVersions()
        }

        val UNKNOWN = CACHED_JAVA_VERSIONS.firstOrNull { it.id == -1 } ?: JavaVersion("unknown", -1)
    }
}

fun getJavaVersionsBetween(javaVersion1: JavaVersion, javaVersion2: JavaVersion): List<JavaVersion> {
    return JavaVersion.values().filter { it.id >= javaVersion1.id && it.id <= javaVersion2.id }.toList()
}

fun getJavaVersion(): JavaVersion {
    val version = System.getProperty("java.version")
    val versionParts = version.split(".")
    val major = versionParts[0].toInt()
    val minor = versionParts[1].toInt()
    return JavaVersion.values().find { it.version == major.toString() } ?: JavaVersion.UNKNOWN
}

fun isJavaVersionSupported(version: JavaVersion): Boolean {
    return version.id == 52 || version.id == 61 || version.id == 62 || version.id == 63
}

fun isJavaVersionNotSupported(version: JavaVersion): Boolean {
    return version.id == 53 || version.id == 54 || version.id == 55 || version.id == 56 || version.id == 57 || version.id == 58 || version.id == 59 || version.id == 60 || version.id == 64
}

fun isJavaVersionUnsupported(version: JavaVersion): Boolean {
    return !isJavaVersionNotSupported(getJavaVersion()) && !isJavaVersionNotSupported(version)
}