package dev.redicloud.cluster.service

import dev.redicloud.utils.ServiceId
import dev.redicloud.utils.ServiceType

data class CloudService(
    val serviceId: ServiceId,
    private val sessions: MutableList<ServiceClusterSession>
){

    fun currentSession(): ServiceClusterSession? {
        if (sessions.isEmpty()) return null
        val last = sessions.last()
        if (last.endTime == -1L) return null
        return last
    }

    fun getSessions(): List<ServiceClusterSession> = sessions.toList()

    fun isConnected(): Boolean = currentSession() != null

    fun firstSession(): ServiceClusterSession? {
        if (sessions.isEmpty()) return null
        return sessions.first()
    }

    fun addSession(session: ServiceClusterSession) {
        sessions.add(session)
        sessions.sortBy { it.startTime }
    }

    fun unregisterAfterDisconnect(): Boolean =
        serviceId.type == ServiceType.SERVER

}