package dev.redicloud.api.repositories.java

interface ICloudJavaVersionInfo {

    val major: Int
    val minor: Int
    val patch: Int
    val build: Int
    val type: String
    val arch: String
    val raw: String
    val versionId: Int

}