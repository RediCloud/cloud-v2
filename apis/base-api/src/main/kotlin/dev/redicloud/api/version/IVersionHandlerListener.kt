package dev.redicloud.api.version

interface IVersionHandlerListener {

    fun onDownloadStart(version: ICloudServerVersion)

    fun onDownloadComplete(version: ICloudServerVersion, error: Boolean)

    fun onPatchStart(version: ICloudServerVersion)

    fun onPatchComplete(version: ICloudServerVersion, error: Boolean)

    fun onProcessStart(name: String, process: Process)

    fun onConnectorDownloadStart(versionType: ICloudServerVersionType)

    fun onConnectorDownloadComplete(versionType: ICloudServerVersionType, error: Boolean)
}
