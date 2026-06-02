package dev.redicloud.utils

/**
 * Represents a signed release manifest (`manifest.json`).
 *
 * The manifest contains version metadata and SHA-256 checksums for every artifact
 * in a release (zip, connectors, node-service JAR, modules). It replaces the flat
 * `checksums.sha256` file and is GPG-signed (`manifest.json.asc`).
 *
 * ## Verification chain
 * ```
 * manifest.json.asc  --GPG verify-->  manifest.json  --SHA-256-->  each artifact
 * ```
 */
data class ManifestInfo(
    /** Base semver version, e.g. `2.4.0`. */
    val version: String,
    /** Full version including channel, build, and git SHA, e.g. `2.4.0-beta.42+abc1234`. */
    val fullVersion: String,
    /** Release channel: `stable`, `beta`, `dev`. */
    val channel: String,
    /** ISO 8601 UTC timestamp of the release build. */
    val timestamp: String,
    /** Minimum required Java version (e.g. `21`). */
    val minJavaVersion: Int,
    /** Map of relative artifact path to its metadata. */
    val artifacts: Map<String, ArtifactInfo>
) {

    /**
     * Looks up the SHA-256 hash for an artifact by its filename (last path segment).
     *
     * This searches all artifact keys and matches by the filename portion,
     * so `redicloud-bukkit-connector-2.4.0.jar` matches the key
     * `connectors/redicloud-bukkit-connector-2.4.0.jar`.
     *
     * @return the lowercase hex SHA-256 hash, or `null` if no matching artifact exists.
     */
    fun findHashByFilename(filename: String): String? =
        artifacts.entries
            .firstOrNull { (key, _) ->
                key == filename || key.substringAfterLast('/') == filename
            }
            ?.value?.sha256

    /**
     * Looks up the [ArtifactInfo] for an artifact by its exact key or filename.
     */
    fun findArtifact(filename: String): ArtifactInfo? =
        artifacts[filename]
            ?: artifacts.entries
                .firstOrNull { (key, _) -> key.substringAfterLast('/') == filename }
                ?.value
}

/**
 * Metadata for a single artifact within the release manifest.
 */
data class ArtifactInfo(
    /** Lowercase hex SHA-256 hash of the artifact. */
    val sha256: String,
    /** File size in bytes. */
    val size: Long,
    /** Artifact type: `distribution`, `connector`, `service`, `module`. */
    val type: String
)
