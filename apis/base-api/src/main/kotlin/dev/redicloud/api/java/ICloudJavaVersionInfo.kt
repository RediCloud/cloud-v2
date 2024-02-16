package dev.redicloud.api.java

interface ICloudJavaVersionInfo {

    val major: Int
    val minor: Int
    val patch: Int
    val raw: String
    val versionId: Int

}