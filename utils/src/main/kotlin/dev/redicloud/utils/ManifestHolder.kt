package dev.redicloud.utils

/**
 * Global holder for the current release manifest.
 *
 * The manifest is loaded and cached by the updater module on startup,
 * and can be queried by any module (e.g. repositories) to verify artifact integrity.
 */
object ManifestHolder {

    /**
     * The manifest for the currently running release.
     * Set by the updater on startup; `null` if not yet loaded or unavailable.
     */
    @Volatile
    var manifest: ManifestInfo? = null
}
