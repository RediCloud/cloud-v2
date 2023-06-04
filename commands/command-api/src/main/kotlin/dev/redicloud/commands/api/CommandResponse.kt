package dev.redicloud.commands.api

data class CommandResponse(val type: CommandResponseType, val message: String? = null, val throwable: Throwable? = null, val usage: String? = null)

enum class CommandResponseType {
    SUCCESS,
    ERROR,
    PERMISSION,
    INVALID_COMMAND,
    INVALID_ARGUMENT_TYPE,
    INVALID_ARGUMENT_COUNT,
    INVALID_SUB_PATH
}