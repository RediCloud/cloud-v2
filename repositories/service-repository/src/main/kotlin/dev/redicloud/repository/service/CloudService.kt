package dev.redicloud.repository.service

import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType

abstract class CloudService(
    val serviceId: ServiceId,
    val name: String,
    private val serviceSessions: ServiceSessions = ServiceSessions(),
    var connected: Boolean = false
){

    fun getIdentifyingName(colored: Boolean = true): String = if (colored) "%hc%$name§8#%tc%${serviceId.id}" else "$name#${serviceId.id}"

    fun currentSession(): ServiceSession? {
        return serviceSessions.currentSession
    }

    fun currentOrLastsession(): ServiceSession? {
        return currentSession() ?: serviceSessions.sessionHistory.lastOrNull()
    }

    fun isSuspended(): Boolean {
        val current = currentSession() ?: return false
        return current.suspended
    }

    fun getSessions(): List<ServiceSession> = serviceSessions.sessionHistory.toMutableList().apply {
        add(currentSession() ?: return@apply)
    }

    fun isConnected(): Boolean = connected && !isSuspended()

    fun registrationSession(): ServiceSession? {
        return serviceSessions.registrationSession
    }

    private fun addSession(session: ServiceSession) {
        serviceSessions.currentSession = session
        if (registrationSession() == null) {
            serviceSessions.registrationSession = session
        }else {
            serviceSessions.sessionHistory.add(session)
            while (serviceSessions.sessionHistory.size > 5) {
                serviceSessions.sessionHistory.removeAt(0)
            }
        }
    }

    fun startSession(ipAddress: String): ServiceSession {
        val session = ServiceSession(this.serviceId, System.currentTimeMillis(), ipAddress)
        addSession(session)
        return session
    }

    fun endSession(session: ServiceSession? = null): ServiceSession {
        val current = session ?: currentSession() ?: throw IllegalStateException("No session is currently active")
        current.endTime = System.currentTimeMillis()
        serviceSessions.currentSession = null
        return current
    }

    open fun unregisterAfterDisconnect(): Boolean =
        serviceId.type == ServiceType.MINECRAFT_SERVER || serviceId.type == ServiceType.PROXY_SERVER

    open fun canSelfUnregister(): Boolean =
        serviceId.type == ServiceType.NODE || serviceId.type == ServiceType.FILE_NODE ||serviceId.type == ServiceType.CLIENT

}