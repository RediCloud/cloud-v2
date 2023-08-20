package dev.redicloud.api.java

import dev.redicloud.api.service.ServiceId
import java.io.File
import java.util.UUID

interface ICloudJavaVersion {

    val uniqueId: UUID
    var name: String
    var id: Int
    val onlineVersion: Boolean
    val located: MutableMap<UUID, String>
    val info: ICloudJavaVersionInfo?
    val unknown: Boolean

    fun isLocated(serviceId: ServiceId): Boolean

    fun autoLocate(): File?

}