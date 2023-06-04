package dev.redicloud.commands.api

import kotlin.reflect.KClass


@Target(AnnotationTarget.CLASS)
annotation class Command(val name: String)

@Target(AnnotationTarget.FUNCTION)
annotation class CommandSubPath(val path: String)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class CommandAlias(val aliases: Array<String>)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class CommandParameter(val name: String = "", val required: Boolean = true, val suggester: KClass<out ICommandSuggester> = EmptySuggester::class, val suggesterArguments: Array<String> = []) //TODO: required will be automatically set to false if the type is nullable

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class CommandDescription(val description: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class CommandVisibleForCompletion

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class CommandPermission(val permission: String)