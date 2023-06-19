package dev.redicloud.repository.server

enum class CloudServerState {

    PREPARING,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    UNKNOWN;

    companion object {
        fun fromString(string: String): CloudServerState {
            return when (string) {
                "STARTING" -> STARTING
                "RUNNING" -> RUNNING
                "STOPPING" -> STOPPING
                "STOPPED" -> STOPPED
                else -> UNKNOWN
            }
        }
    }

}