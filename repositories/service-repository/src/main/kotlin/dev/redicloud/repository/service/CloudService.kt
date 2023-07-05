package dev.redicloud.repository.service

import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType

abstract class CloudService(
    val serviceId: ServiceId,
    val name: String,
    private val sessions: MutableList<ServiceSession>,
    var connected: Boolean = false
){

    fun getIdentifyingName(colored: Boolean = true): String = if (colored) "%hc%$name§8#%tc%${serviceId.id}" else "$name#${serviceId.id}"

    fun currentSession(): ServiceSession? {
        if (sessions.isEmpty()) return null
        val last = sessions.last()
        if (last.endTime != -1L) return null
        return last
    }

    fun currentOrLastsession(): ServiceSession? {
        if (sessions.isEmpty()) return null
        return sessions.last()
    }

    fun isSuspended(): Boolean {
        val current = currentSession() ?: return false
        return current.suspended
    }

    fun getSessions(): List<ServiceSession> = sessions.toList()

    fun isConnected(): Boolean = connected && !isSuspended()

    fun firstSession(): ServiceSession? {
        if (sessions.isEmpty()) return null
        return sessions.first()
    }

    private fun addSession(session: ServiceSession) {
        sessions.add(session)
        sessions.sortBy { it.startTime }
    }

    fun startSession(ipAddress: String): ServiceSession {
        val session = ServiceSession(this.serviceId, System.currentTimeMillis(), ipAddress = ipAddress)
        addSession(session)
        return session
    }

    fun endSession(session: ServiceSession? = null): ServiceSession {
        val current = session ?: currentSession() ?: throw IllegalStateException("No session is currently active")
        current.endTime = System.currentTimeMillis()
        return current
    }

    open fun unregisterAfterDisconnect(): Boolean =
        serviceId.type == ServiceType.MINECRAFT_SERVER || serviceId.type == ServiceType.PROXY_SERVER

    open fun canSelfUnregister(): Boolean =
        serviceId.type == ServiceType.NODE || serviceId.type == ServiceType.FILE_NODE ||serviceId.type == ServiceType.CLIENT

}