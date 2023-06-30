package dev.redicloud.utils

private val osName: String = System.getProperty("os.name").lowercase()

fun getOperatingSystemType(): OSType {
    return when {
        osName.contains("win") -> OSType.WINDOWS
        osName.contains("mac") || osName.contains("darwin") -> OSType.MACOS
        osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OSType.LINUX
        osName.contains("sunos") -> OSType.SOLARIS
        else -> OSType.UNKNOWN
    }
}

enum class OSType {
    WINDOWS,
    MACOS,
    LINUX,
    SOLARIS,
    UNKNOWN
}