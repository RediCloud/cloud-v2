package dev.redicloud.repository.java.version

import dev.redicloud.api.java.ICloudJavaVersionInfo

class JavaVersionInfo(
    override val major: Int,
    override val minor: Int,
    override val patch: Int,
    override val raw: String
) : ICloudJavaVersionInfo {

    override val versionId: Int
        get() {
            return toVersionId(major)
        }

}