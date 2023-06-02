package dev.redicloud.repository.service

import dev.redicloud.utils.ServiceId
import dev.redicloud.utils.ServiceType

abstract class CloudService(
    val serviceId: ServiceId,
    private val sessions: MutableList<ServiceSession>
){

    fun currentSession(): ServiceSession? {
        if (sessions.isEmpty()) return null
        val last = sessions.last()
        if (last.endTime == -1L) return null
        return last
    }

    fun getSessions(): List<ServiceSession> = sessions.toList()

    fun isConnected(): Boolean = currentSession() != null

    fun firstSession(): ServiceSession? {
        if (sessions.isEmpty()) return null
        return sessions.first()
    }

    fun addSession(session: ServiceSession) {
        sessions.add(session)
        sessions.sortBy { it.startTime }
    }

    fun unregisterAfterDisconnect(): Boolean =
        serviceId.type == ServiceType.SERVER

}