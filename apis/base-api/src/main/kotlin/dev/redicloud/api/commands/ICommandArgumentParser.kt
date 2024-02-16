package dev.redicloud.api.commands

interface ICommandArgumentParser<T> {
    fun parse(parameter: String): T?
}