package dev.redicloud.api.version

/**
 * Listener for server version handler lifecycle events.
 *
 * Implementations receive callbacks during download, patch, and connector
 * operations. This decouples the data/repository layer from the presentation
 * layer -- the console module provides a concrete implementation that
 * displays progress animations.
 *
 * Obtain an instance via Guice injection on node services.
 */
interface IVersionHandlerListener {

    /**
     * Called when a server version download starts.
     *
     * @param version the version being downloaded
     */
    fun onDownloadStart(version: ICloudServerVersion)

    /**
     * Called when a server version download completes.
     *
     * @param version the version that was downloaded
     * @param error `true` if the download failed
     */
    fun onDownloadComplete(version: ICloudServerVersion, error: Boolean)

    /**
     * Called when a server version patch starts.
     *
     * @param version the version being patched
     */
    fun onPatchStart(version: ICloudServerVersion)

    /**
     * Called when a server version patch completes.
     *
     * @param version the version that was patched
     * @param error `true` if the patch failed
     */
    fun onPatchComplete(version: ICloudServerVersion, error: Boolean)

    /**
     * Called when a subprocess is started during patching.
     *
     * Implementations may capture the process output for display.
     *
     * @param name a descriptive name for the process (e.g. `"patch_paper-1.20"`)
     * @param process the running subprocess
     */
    fun onProcessStart(name: String, process: Process)

    /**
     * Called when a connector download starts.
     *
     * @param versionType the server version type whose connector is being downloaded
     */
    fun onConnectorDownloadStart(versionType: ICloudServerVersionType)

    /**
     * Called when a connector download completes.
     *
     * @param versionType the server version type whose connector was downloaded
     * @param error `true` if the download failed
     */
    fun onConnectorDownloadComplete(versionType: ICloudServerVersionType, error: Boolean)
}
