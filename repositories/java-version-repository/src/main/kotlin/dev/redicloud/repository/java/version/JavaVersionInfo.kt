package dev.redicloud.repository.java.version

import dev.redicloud.api.repositories.java.ICloudJavaVersionInfo

class JavaVersionInfo(
    override val major: Int,
    override val minor: Int,
    override val patch: Int,
    override val build: Int,
    override val type: String,
    override val arch: String,
    override val raw: String
) : ICloudJavaVersionInfo {

    override val versionId: Int
        get() {
            return toVersionId(major)
        }

}