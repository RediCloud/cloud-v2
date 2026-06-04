package dev.redicloud.console.utils

import dev.redicloud.api.version.ICloudServerVersion
import dev.redicloud.api.version.ICloudServerVersionType
import dev.redicloud.api.version.IVersionHandlerListener
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.utils.toConsoleValue

class ConsoleVersionHandlerListener(
    private val console: Console
) : IVersionHandlerListener {

    companion object {
        private const val ANIMATION_TICK_MS = 200L
    }

    private var downloadCanceled = false
    private var downloadDone = false
    private var downloadError = false
    private var currentDownloadVersion: ICloudServerVersion? = null

    private var patchCanceled = false
    private var patchDone = false
    private var patchError = false
    private var currentPatchVersion: ICloudServerVersion? = null

    private var connectorCanceled = false
    private var connectorDone = false
    private var connectorError = false
    private var currentConnectorType: ICloudServerVersionType? = null

    override fun onDownloadStart(version: ICloudServerVersion) {
        downloadCanceled = false
        downloadDone = false
        downloadError = false
        currentDownloadVersion = version
        val animation = AnimatedLineAnimation(console, ANIMATION_TICK_MS) {
            if (downloadCanceled) {
                null
            } else if (downloadDone) {
                downloadCanceled = true
                "Downloaded version %hc%${version.displayName}§8: ${if (downloadError) "§4✘" else "§2✓"}"
            } else {
                "Downloading version %hc%${version.displayName}§8: %tc%%loading%"
            }
        }
        console.startAnimation(animation)
    }

    override fun onDownloadComplete(version: ICloudServerVersion, error: Boolean) {
        downloadError = error
        downloadDone = true
    }

    override fun onPatchStart(version: ICloudServerVersion) {
        patchCanceled = false
        patchDone = false
        patchError = false
        currentPatchVersion = version
        val animation = AnimatedLineAnimation(console, ANIMATION_TICK_MS) {
            if (patchCanceled) {
                null
            } else if (patchDone) {
                patchCanceled = true
                "Patching version %tc%${toConsoleValue(version.displayName)}§8: ${if (patchError) "§4✘" else "§2✓"}"
            } else {
                "Patching version %tc%${toConsoleValue(version.displayName)}§8: %tc%%loading%"
            }
        }
        console.startAnimation(animation)
    }

    override fun onPatchComplete(version: ICloudServerVersion, error: Boolean) {
        patchError = error
        patchDone = true
    }

    override fun onProcessStart(name: String, process: Process) {
        val screen = console.createScreen(name)
        ScreenProcessHandler(process, screen)
    }

    override fun onConnectorDownloadStart(versionType: ICloudServerVersionType) {
        connectorCanceled = false
        connectorDone = false
        connectorError = false
        currentConnectorType = versionType
        val animation = AnimatedLineAnimation(console, ANIMATION_TICK_MS) {
            if (connectorCanceled) {
                null
            } else if (connectorDone) {
                connectorCanceled = true
                "Downloaded connector ${toConsoleValue(versionType.name)}§8: ${if (connectorError) "§4✘" else "§2✓"}"
            } else {
                "Downloading connector ${toConsoleValue(versionType.name)}§8: %tc%%loading%"
            }
        }
        console.startAnimation(animation)
    }

    override fun onConnectorDownloadComplete(versionType: ICloudServerVersionType, error: Boolean) {
        connectorError = error
        connectorDone = true
    }
}
