package dev.redicloud.repository.java.version

import dev.redicloud.utils.OSType
import dev.redicloud.utils.getOperatingSystemType
import java.io.File

fun getJavaVersionsBetween(javaVersion1: JavaVersion, javaVersion2: JavaVersion): List<JavaVersion> {
    return JavaVersionRepository.ONLINE_VERSION_CACHE.get()!!
        .filter { it.id >= javaVersion1.id && it.id <= javaVersion2.id }.toList()
}

fun getJavaVersion(): JavaVersion {
    val version = System.getProperty("java.version")
    val versionParts = version.split(".")
    val major = versionParts[0].toInt()
    val minor = versionParts[1].toInt()
    return JavaVersionRepository.ONLINE_VERSION_CACHE.get()!!.find { it.name == major.toString() }
        ?: JavaVersionRepository.ONLINE_VERSION_CACHE.get()!!.first { it.isUnknown() }
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

fun parseVersionInfo(version: String): JavaVersionInfo? {
    var matchResult = Regex("(?i)(jdk|jre)([0-9]+)\\.([0-9]+)\\.([0-9]+)_([0-9]+)").find(version)
    //%type%%major%.%minor%.%patch%_%build% Beispiel: jre1.8.0_202
    //%type%%major%.%minor%.%patch% Beispiel: jre1.8.0
    if (matchResult != null) {
        println("Matcher 1")
        val type = matchResult.groupValues[1]
        val major = matchResult.groupValues[2].toInt()
        val minor = matchResult.groupValues[3].toInt()
        val patch = matchResult.groupValues[4].toInt()
        val build = matchResult.groupValues[5].toInt()

        return JavaVersionInfo(major, minor, patch, build, type, "unknown", version)
    }
    matchResult = Regex("(?i)(jdk|jre)(-)([0-9]+)(.)([0-9]+)(.)([0-9]+)").find(version)
    //%type%-%major%.%minor%.%patch% Beispiel: jdk-17.0.2
    if (matchResult != null) {
        println("Matcher 2")
        val type = matchResult.groupValues[1]
        val major = matchResult.groupValues[3].toInt()
        val minor = matchResult.groupValues[5].toInt()
        val patch = matchResult.groupValues[7].toInt()

        return JavaVersionInfo(major, minor, patch, -1, type, "unknown", version)
    }
    matchResult = Regex("(?i)java-([0-9]+)-open(jdk|jre)-(\\w+)").find(version)
    //%type%%major%.%minor%.%patch% Beispiel: jre1.8.0
    if (matchResult != null) {
        println("Matcher 3")
        val major = matchResult.groupValues[1].toInt()
        val type = matchResult.groupValues[2].toUpperCase()
        val arch = matchResult.groupValues[3]
        return JavaVersionInfo(major, 0, 0, -1, type, arch, version)
    }
    matchResult = Regex("(?i)java-([0-9]+)\\.([0-9]+)\\.([0-9]+)-open(jdk|jre)-(\\w+)").find(version)
    //java-%major%-open%type%-%arch% Beispiel: java-17-openjdk-amd64
    if (matchResult != null) {
        println("Matcher 4")
        val major = matchResult.groupValues[1].toInt()
        val minor = matchResult.groupValues[2].toInt()
        val patch = matchResult.groupValues[3].toInt()
        val type = matchResult.groupValues[4].toUpperCase()
        val arch = matchResult.groupValues[5]
        return JavaVersionInfo(major, minor, patch, -1, type, arch, version)
    }

    return null
}

fun locateAllJavaVersions(): List<File> {
    val versionFolders = mutableListOf<File>()

    when (getOperatingSystemType()) {
        OSType.WINDOWS -> {
            mutableListOf<String>(
                "\\Program Files\\Java",
                "\\Program Files (x86)\\Java"
            ).filter {
                it.isNotEmpty()
            }.map {
                File(it)
            }.filter {
                val state = it.exists()
                state
            }.filter { it.isDirectory }
                .forEach { versionFolders.addAll(it.listFiles()!!.toList()) }
        }
        OSType.LINUX -> {
            mutableListOf<String>(
                "/usr/lib/jvm",
                "/usr/lib64/jvm"
            ).filter { it.isNotEmpty() }.map { File(it) }.filter { it.exists() }.filter { it.isDirectory }
                .forEach { versionFolders.addAll(it.listFiles()!!.toList()) }
        }
        else -> {}
    }

    if (System.getProperty("redicloud.java.versions.path", "").isNotEmpty()) {
        File(System.getProperty("redicloud.java.versions.path")).listFiles()!!.toList().forEach { versionFolders.add(it) }
    }

    return versionFolders
}

fun toVersionId(versionNumber: Int): Int {
    return versionNumber+44
}

fun toVersionNumber(versionId: Int): Int {
    return versionId-44
}

data class JavaVersionInfo(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val build: Int,
    val type: String,
    val arch: String,
    val raw: String
) {
    fun toVersionId(): Int = toVersionId(major)
}