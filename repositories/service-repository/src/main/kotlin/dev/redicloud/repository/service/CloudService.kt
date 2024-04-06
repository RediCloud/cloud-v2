package dev.redicloud.repository.service

import dev.redicloud.api.service.ICloudService
import dev.redicloud.api.service.ICloudServiceSession
import dev.redicloud.cache.IClusterCacheObject
import dev.redicloud.api.service.ServiceId
import dev.redicloud.utils.isIpv4
import dev.redicloud.utils.isIpv6

abstract class CloudService(
    override val serviceId: ServiceId,
    override val name: String,
    val sessions: ServiceSessions = ServiceSessions(),
    override var connected: Boolean = false
) : IClusterCacheObject, ICloudService {

    override fun identifyName(colored: Boolean): String
        = if (colored) "%hc%$name§8#%tc%${serviceId.id}" else "$name#${serviceId.id}"

    override val currentSession: ServiceSession?
        get() {
            return sessions.currentSession
        }

    override val registrationSession: ServiceSession?
        get() {
            return sessions.registrationSession
        }

    override fun currentOrLastSession(): ServiceSession? {
        return currentSession ?: sessions.sessionHistory.lastOrNull()
    }

    override val sessionHistory: List<ServiceSession>
        get() {
            return sessions.sessionHistory.toMutableList().apply {
                currentSession?.let { add(it) }
            }
        }

    override fun endSession(session: ICloudServiceSession?): ICloudServiceSession {
        val current = (session ?: currentSession) as ServiceSession? ?: throw IllegalStateException("No session is currently active")
        current.endTime = System.currentTimeMillis()
        sessions.currentSession = null
        return current
    }

    override val suspended: Boolean
        get() {
            return sessions.currentSession?.suspended ?: false
        }

    override fun startSession(ipAddress: String): ServiceSession {
        if (!isIpv4(ipAddress) && !isIpv6(ipAddress)) {
            throw IllegalArgumentException("Invalid IP address: $ipAddress")
        }
        var hostname = ipAddress
        if (isIpv6(ipAddress) && !ipAddress.startsWith("[")) {
            hostname = "[$ipAddress]"
        }
        val session = ServiceSession(this.serviceId, System.currentTimeMillis(), hostname)
        sessions.currentSession = session
        if (registrationSession == null) {
            sessions.registrationSession = session
        }else {
            sessions.sessionHistory.add(session)
            while (sessions.sessionHistory.size > 5) {
                sessions.sessionHistory.removeAt(0)
            }
        }
        return session
    }

}