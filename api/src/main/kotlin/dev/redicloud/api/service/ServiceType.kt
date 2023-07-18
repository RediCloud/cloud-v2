package dev.redicloud.api.service

enum class ServiceType {

    NODE,
    FILE_NODE,
    MINECRAFT_SERVER,
    PROXY_SERVER,
    CLIENT;

    fun isServer(): Boolean {
        return this == MINECRAFT_SERVER || this == PROXY_SERVER
    }

}