package dev.redicloud.updater

import com.google.gson.annotations.SerializedName
import dev.redicloud.utils.version.CloudVersion
import dev.redicloud.utils.version.VersionChannel

/**
 * Represents a GitHub release mapped to RediCloud's version model.
 */
data class ReleaseInfo(
    val version: CloudVersion,
    val tagName: String,
    val zipUrl: String?,
    val checksumsUrl: String?,
    val signatureUrl: String?,
    val signingKeyUrl: String?
) {
    val channel: VersionChannel
        get() = version.channel
}

/** Subset of the GitHub Releases API response. */
internal data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    val name: String?,
    val prerelease: Boolean,
    val draft: Boolean,
    val assets: List<GitHubAsset>
)

/** A single asset attached to a GitHub release. */
internal data class GitHubAsset(
    val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
    val size: Long
)
