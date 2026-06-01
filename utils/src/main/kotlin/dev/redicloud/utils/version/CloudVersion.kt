package dev.redicloud.utils.version

/**
 * A RediCloud version following [SemVer 2.0.0](https://semver.org/).
 *
 * Format: `<major>.<minor>.<patch>[-<channel>.<build>][+<gitSha>]`
 *
 * Examples:
 * - `2.4.0-dev.42+a1b2c3d` -- dev build
 * - `2.4.0-beta.3+a1b2c3d` -- beta pre-release
 * - `2.4.0+a1b2c3d` -- stable release
 * - `2.4.0` -- stable, no metadata
 *
 * Ordering follows SemVer:
 * `2.4.0-dev.1 < 2.4.0-dev.42 < 2.4.0-beta.1 < 2.4.0-beta.3 < 2.4.0`
 *
 * Build metadata (`+gitSha`) is ignored for ordering.
 */
data class CloudVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val channel: VersionChannel = VersionChannel.STABLE,
    val build: Int = 0,
    val gitSha: String = ""
) : Comparable<CloudVersion> {

    /** Base version without channel or metadata, e.g. `2.4.0`. */
    val base: String
        get() = "$major.$minor.$patch"

    /** Pre-release segment, e.g. `dev.42` or `beta.3`. `null` for stable. */
    val preRelease: String?
        get() = if (channel.isPreRelease) "${channel.label}.$build" else null

    /**
     * Full version string including metadata, e.g. `2.4.0-beta.3+a1b2c3d`.
     * Stable without metadata returns just the base: `2.4.0`.
     */
    val full: String
        get() = buildString {
            append(base)
            preRelease?.let { append('-').append(it) }
            if (gitSha.isNotBlank()) append('+').append(gitSha)
        }

    /** Version without build metadata (for artifact naming), e.g. `2.4.0-beta.3` or `2.4.0`. */
    val display: String
        get() = buildString {
            append(base)
            preRelease?.let { append('-').append(it) }
        }

    /**
     * SemVer ordering.
     *
     * 1. Compare major.minor.patch numerically.
     * 2. Pre-release versions have lower precedence than stable (`2.4.0-beta.3 < 2.4.0`).
     * 3. Among pre-releases: compare channel order, then build number.
     * 4. Build metadata (`+gitSha`) is ignored.
     */
    override fun compareTo(other: CloudVersion): Int {
        var cmp = major.compareTo(other.major)
        if (cmp != 0) return cmp
        cmp = minor.compareTo(other.minor)
        if (cmp != 0) return cmp
        cmp = patch.compareTo(other.patch)
        if (cmp != 0) return cmp

        // SemVer: stable > any pre-release
        if (channel == VersionChannel.STABLE && other.channel != VersionChannel.STABLE) return 1
        if (channel != VersionChannel.STABLE && other.channel == VersionChannel.STABLE) return -1

        cmp = channel.order.compareTo(other.channel.order)
        if (cmp != 0) return cmp

        return build.compareTo(other.build)
    }

    override fun toString(): String = full

    companion object {

        private val VERSION_REGEX = Regex(
            """^(\d+)\.(\d+)\.(\d+)(?:-(dev|beta)\.(\d+))?(?:\+([a-fA-F0-9]+))?$"""
        )

        /**
         * Parses a version string.
         *
         * @throws IllegalArgumentException if the format is invalid.
         */
        fun parse(versionString: String): CloudVersion {
            val match = VERSION_REGEX.matchEntire(versionString.trim())
                ?: throw IllegalArgumentException(
                    "Invalid version format: '$versionString'. " +
                        "Expected: <major>.<minor>.<patch>[-<channel>.<build>][+<gitSha>]"
                )

            val (majorStr, minorStr, patchStr, channelStr, buildStr, gitSha) = match.destructured

            return CloudVersion(
                major = majorStr.toInt(),
                minor = minorStr.toInt(),
                patch = patchStr.toInt(),
                channel = if (channelStr.isBlank()) VersionChannel.STABLE else VersionChannel.fromLabel(channelStr),
                build = if (buildStr.isBlank()) 0 else buildStr.toInt(),
                gitSha = gitSha
            )
        }

        /** Parses a version string, returning `null` on invalid format. */
        fun parseOrNull(versionString: String): CloudVersion? =
            runCatching { parse(versionString) }.getOrNull()

        /**
         * Constructs a version from a base version string plus individual components.
         *
         * Intended for the CI/build system, e.g. `CloudVersion.of("2.4.0", BETA, 3, "a1b2c3d")`.
         *
         * @throws IllegalArgumentException if [baseVersion] is not `<major>.<minor>.<patch>`.
         */
        fun of(
            baseVersion: String,
            channel: VersionChannel = VersionChannel.DEV,
            build: Int = 0,
            gitSha: String = ""
        ): CloudVersion {
            val parts = baseVersion.split('.')
            require(parts.size == 3) {
                "Base version must be <major>.<minor>.<patch>, got: '$baseVersion'"
            }
            return CloudVersion(
                major = parts[0].toInt(),
                minor = parts[1].toInt(),
                patch = parts[2].toInt(),
                channel = channel,
                build = build,
                gitSha = gitSha
            )
        }
    }
}
