package dev.redicloud.api.commands

interface ICommandArgumentParser<T> {
    suspend fun parse(parameter: String): T?
}
