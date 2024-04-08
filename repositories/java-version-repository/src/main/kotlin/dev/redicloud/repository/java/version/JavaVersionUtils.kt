package dev.redicloud.repository.java.version

import dev.redicloud.utils.OSType
import dev.redicloud.utils.getOperatingSystemType
import java.io.File

fun getJavaVersionsBetween(javaVersion1: CloudJavaVersion, javaVersion2: CloudJavaVersion): List<CloudJavaVersion> {
    return JavaVersionRepository.ONLINE_VERSION_CACHE.get()!!
        .filter { it.id >= javaVersion1.id && it.id <= javaVersion2.id }.toList()
}

fun getJavaVersion(): CloudJavaVersion {
    val version = System.getProperty("java.version")
    if (version.contains("-") && version.split(".").size < 2) {
        return JavaVersionRepository.ONLINE_VERSION_CACHE.get()!!.find { it.name == version.split("-")[0] }
            ?: JavaVersionRepository.ONLINE_VERSION_CACHE.get()!!.first { it.unknown }
    }
    val versionParts = version.split(".")
    val major = versionParts[0].toInt()
    val minor = versionParts[1].toInt()
    return JavaVersionRepository.ONLINE_VERSION_CACHE.get()!!.find { it.name == major.toString() }
        ?: JavaVersionRepository.ONLINE_VERSION_CACHE.get()!!.first { it.unknown }
}

fun isJavaVersionSupported(version: CloudJavaVersion): Boolean {
    return version.id == 52 || version.id == 61 || version.id == 62 || version.id == 63
}

fun isJavaVersionNotSupported(version: CloudJavaVersion): Boolean {
    return version.id == 53 || version.id == 54 || version.id == 55 || version.id == 56 || version.id == 57 || version.id == 58 || version.id == 59 || version.id == 60 || version.id == 64
}

fun isJavaVersionUnsupported(version: CloudJavaVersion): Boolean {
    return !isJavaVersionNotSupported(getJavaVersion()) && !isJavaVersionNotSupported(version)
}

suspend fun getVersionInfo(path: String): JavaVersionInfo? {
    var end = "bin" + File.separator + (if (getOperatingSystemType() == OSType.WINDOWS) "java.exe" else "java")
    if (!path.endsWith(File.separator)) end = File.separator + end
    val processBuilder = ProcessBuilder(path + (end), "-version")
    processBuilder.redirectErrorStream(true)
    val process = processBuilder.start()
    val reader = process.inputStream.bufferedReader()
    val output = reader.readLines()
    if (output.size >= 5) return null
    val versionParts = output[1].split("(build ").last().split("+").first().split(".")
    val major = versionParts[0]
    val minor = versionParts[1]
    val patch = versionParts[2]
    if (major.toIntOrNull() == null || minor.toIntOrNull() == null || patch.toIntOrNull() == null) return null
    return JavaVersionInfo(major.toInt(), minor.toInt(), patch.toInt(), output.joinToString("\n"))
}

fun locateAllJavaVersions(): List<File> {
    val versionFolders = mutableListOf<File>()

    val paths = mutableListOf<String>()
    if (System.getProperty("redicloud.java.versions.path", "").isNotEmpty()) {
        paths.add(System.getProperty("redicloud.java.versions.path", ""))
    }
    if (System.getenv().containsKey("JAVA_HOME")) {
        val homePathSplit = System.getenv("JAVA_HOME").split(File.separator)
        paths.add(homePathSplit.subList(0, homePathSplit.size - 2).joinToString(File.separator))
    }
    if (System.getenv().containsKey("JAVA_INSTALLATIONS_FOLDER")) {
        paths.add(System.getenv("JAVA_INSTALLATIONS_FOLDER"))
    }

    when (getOperatingSystemType()) {
        OSType.WINDOWS -> {
            paths.add("\\Program Files\\Java")
            paths.add("\\Program Files (x86)\\Java")
        }
        OSType.LINUX -> {
            paths.add("/usr/lib/jvm")
            paths.add("/usr/lib64/jvm")
        }
        else -> {}
    }

    paths.filter {
        it.isNotEmpty()
    }.map {
        File(it)
    }.filter {
        val state = it.exists()
        state
    }.filter { it.isDirectory }
        .forEach { versionFolders.addAll(it.listFiles()!!.toList()) }

    return versionFolders
}

fun toVersionId(versionNumber: Int): Int {
    return versionNumber+44
}

fun toVersionNumber(versionId: Int): Int {
    return versionId-44
}