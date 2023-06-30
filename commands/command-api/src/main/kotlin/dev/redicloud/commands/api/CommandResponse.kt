package dev.redicloud.commands.api

data class CommandResponse(val type: CommandResponseType, val message: String? = null, val throwable: Throwable? = null, val usage: String? = null)

enum class CommandResponseType {
    SUCCESS,
    DISABLED,
    ERROR,
    PERMISSION,
    INVALID_COMMAND,
    BLANK_INPUT,
    INVALID_ARGUMENT_TYPE,
    INVALID_ARGUMENT_COUNT,
    HELP_SENT,
    INVALID_SUB_PATH
}