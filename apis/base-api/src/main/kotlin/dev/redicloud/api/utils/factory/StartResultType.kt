package dev.redicloud.api.utils.factory

enum class StartResultType {
    SUCCESS,
    ALREADY_RUNNING,
    RAM_USAGE_TOO_HIGH,
    TOO_MUCH_SERVICES_OF_TEMPLATE,
    NODE_IS_NOT_ALLOWED,
    NODE_NOT_CONNECTED,
    UNKNOWN_SERVER_VERSION,
    UNKNOWN_SERVER_TYPE_VERSION,
    UNKNOWN_SERVER_VERSION_HANDLER,
    UNKNOWN_JAVA_VERSION,
    UNKNOWN_CONFIGURATION_TEMPLATE,
    UNKNOWN_ERROR,
    STOPPED,
    JAVA_VERSION_NOT_INSTALLED
}