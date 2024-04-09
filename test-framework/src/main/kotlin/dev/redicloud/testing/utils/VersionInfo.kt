package dev.redicloud.testing.utils

data class VersionInfo(
    var branch: String,
    var build: String = "latest"
) {
    companion object {
        val LATEST_STABLE = VersionInfo("master", "latest")
        val LATEST_DEVELOPMENT = VersionInfo("dev", "latest")
    }
}