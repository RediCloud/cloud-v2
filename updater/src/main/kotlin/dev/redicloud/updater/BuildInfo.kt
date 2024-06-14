package dev.redicloud.updater

data class BuildInfo(
    val branch: String,
    val build: Int,
    val version: String,
    val workflowId: Long = -1,
    val stored: Boolean = false
) {

    fun isOlderThan(other: BuildInfo): Boolean {
        if (this.isSameVersion(other)) return false

        val thisVersionNumbers = version.split("-")[0].split(".").map { it.toInt() }
        val otherVersionNumbers = other.version.split("-")[0].split(".").map { it.toInt() }

        val thisRelease = this.version.split("-")[1] == "RELEASE"
        val otherRelease = other.version.split("-")[1] == "RELEASE"

        val thisMajor = thisVersionNumbers[0]
        val otherMajor = otherVersionNumbers[0]

        val thisMinor = thisVersionNumbers[1]
        val otherMinor = otherVersionNumbers[1]

        val thisPatch = thisVersionNumbers[2]
        val otherPatch = otherVersionNumbers[2]

        if (thisMajor > otherMajor) return false
        if (thisMajor < otherMajor) return true

        if (thisMinor > otherMinor) return false
        if (thisMinor < otherMinor) return true

        if (thisPatch > otherPatch) return false
        if (thisPatch < otherPatch) return true

        if (thisRelease && !otherRelease) return false
        if (!thisRelease && otherRelease) return true

        return false
    }

    fun isSameVersion(other: BuildInfo): Boolean {
        return version == other.version
    }

}