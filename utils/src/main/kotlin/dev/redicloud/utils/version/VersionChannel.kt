package dev.redicloud.utils.version

/**
 * Release channels for RediCloud builds.
 *
 * Ordered by stability: [DEV] < [BETA] < [STABLE].
 */
enum class VersionChannel(val label: String, val order: Int) {
    DEV("dev", 0),
    BETA("beta", 1),
    STABLE("stable", 2);

    /** Whether this channel produces pre-release versions. */
    val isPreRelease: Boolean
        get() = this != STABLE

    companion object {

        /**
         * Resolves a channel from its label (case-insensitive).
         *
         * @throws IllegalArgumentException if the label does not match any channel.
         */
        fun fromLabel(label: String): VersionChannel =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Unknown version channel: '$label'. Valid: ${entries.joinToString { it.label }}"
                )

        /** Resolves a channel from its label, returning `null` on mismatch. */
        fun fromLabelOrNull(label: String): VersionChannel? =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
    }
}
