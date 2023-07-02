package dev.redicloud.repository.server

enum class CloudServerState(
    val displayName: String,
    val color: String
) {

    PREPARING("preparing", "§e"),
    STARTING("starting", "§6"),
    RUNNING("running", "§2"),
    STOPPING("stopping", "§c"),
    STOPPED("stopped", "§4"),
    UNKNOWN("unknown", "§f");

    companion object {
        fun fromString(string: String): CloudServerState {
            return when (string.uppercase()) {
                "STARTING" -> STARTING
                "RUNNING" -> RUNNING
                "STOPPING" -> STOPPING
                "STOPPED" -> STOPPED
                else -> UNKNOWN
            }
        }
    }

}