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

fun locateAllJavaVersions(): List<File> {
    val versionFolders = mutableListOf<File>()

    when (getOperatingSystemType()) {
        OSType.WINDOWS -> {
            mutableListOf<String>(
                "Program Files\\Java",
                "Program Files (x86)\\Java"
            ).filter { it.isNotEmpty() }.map { File(it) }.filter { it.exists() }.filter { it.isDirectory }
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