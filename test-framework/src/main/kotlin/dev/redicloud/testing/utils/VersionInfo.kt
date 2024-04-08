package dev.redicloud.testing.utils

data class VersionInfo(
    var branch: String,
    var build: String
) {
    companion object {
        val LATEST_STABLE = VersionInfo("master", "lastest")
        val LATEST_DEVELOPMENT = VersionInfo("dev", "lastest")
    }
}