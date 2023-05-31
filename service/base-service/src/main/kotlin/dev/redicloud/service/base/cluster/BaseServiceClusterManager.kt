package dev.redicloud.service.base.cluster

import dev.redicloud.cluster.service.CloudService
import dev.redicloud.cluster.service.ServiceClusterManager
import dev.redicloud.cluster.service.ServiceClusterSession
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.utils.ServiceId

class BaseServiceClusterManager(databaseConnection: DatabaseConnection, serviceId: ServiceId)
    : ServiceClusterManager(databaseConnection, serviceId) {

    internal suspend fun connect() {
        if (!registeredServices.contains(serviceId)) {
            registeredServices.add(serviceId)
        }
        connectedServices.add(serviceId)

        Runtime.getRuntime().addShutdownHook(shutdownThread)

        var service = getService(serviceId)
        if (service == null) {
            service =
                CloudService(serviceId, mutableListOf(ServiceClusterSession(serviceId, System.currentTimeMillis())))
        } else {
            service.addSession(ServiceClusterSession(serviceId, System.currentTimeMillis()))
        }
        set(serviceId.toName(), service)
    }

    internal suspend fun disconnect() {
        if (!connectedServices.contains(serviceId)) return
        connectedServices.remove(serviceId)

        val service = getService(serviceId)
        if (service == null) return
        if (service.isConnected()) {
            service.currentSession()!!.endTime = System.currentTimeMillis()
        }
        if (service.unregisterAfterDisconnect()) {
            registeredServices.remove(serviceId)
            delete(serviceId.toName())
        } else {
            set(serviceId.toName(), service)
        }
    }

}