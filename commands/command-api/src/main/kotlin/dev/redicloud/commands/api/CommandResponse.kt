package dev.redicloud.commands.api

data class CommandResponse(val type: CommandResponseType, val message: String? = null, val exception: Throwable? = null)

enum class CommandResponseType {
    SUCCESS,
    ERROR,
    INVALID_COMMAND,
    INVALID_ARGUMENT_TYPE,
    INVALID_ARGUMENT_COUNT,
    INVALID_SUB_PATH
}