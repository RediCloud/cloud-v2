package dev.redicloud.api.repositories.java

import java.util.UUID

interface ICloudJavaVersionRepository {

    suspend fun getVersion(uniqueId: UUID): ICloudJavaVersion?

    suspend fun getVersion(name: String): ICloudJavaVersion?

    suspend fun existsVersion(uniqueId: UUID): Boolean

    suspend fun existsVersion(name: String): Boolean

    suspend fun createVersion(javaVersion: ICloudJavaVersion): ICloudJavaVersion

    suspend fun updateVersion(javaVersion: ICloudJavaVersion): ICloudJavaVersion

    suspend fun deleteVersion(javaVersion: ICloudJavaVersion): Boolean

    suspend fun deleteVersion(uniqueId: UUID): Boolean

    suspend fun getVersions(): List<ICloudJavaVersion>

    suspend fun getOnlineVersions(): List<ICloudJavaVersion>

    suspend fun detectInstalledVersions(): List<ICloudJavaVersion>

}