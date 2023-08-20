package dev.redicloud.api.service

interface ICloudService {

    val serviceId: ServiceId
    val name: String
    val connected: Boolean
    val suspended: Boolean
    val currentSession: ICloudServiceSession?
    val registrationSession: ICloudServiceSession?
    val sessionHistory: List<ICloudServiceSession>

    fun identifyName(colored: Boolean = true): String

    fun currentOrLastSession(): ICloudServiceSession?

    fun startSession(ipAddress: String): ICloudServiceSession

    fun endSession(session: ICloudServiceSession? = null): ICloudServiceSession

    fun unregisterAfterDisconnect(): Boolean =
        serviceId.type == ServiceType.MINECRAFT_SERVER || serviceId.type == ServiceType.PROXY_SERVER

    fun canSelfUnregister(): Boolean =
        serviceId.type == ServiceType.NODE || serviceId.type == ServiceType.FILE_NODE ||serviceId.type == ServiceType.CLIENT

}