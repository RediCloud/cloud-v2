package dev.redicloud.updater

data class UpdateInfo(
    val version: String,
    val branch: String,
    val build: String,
    val oldVersion: String,
    val oldBranch: String,
    val oldBuild: String
) {

    fun toNewVersionBuildInfo(): BuildInfo {
        return BuildInfo(branch, build.toInt(), version, 0, true)
    }

    fun toOldVersionBuildInfo(): BuildInfo {
        return BuildInfo(oldBranch, oldBuild.toInt(), oldVersion, 0, true)
    }

}