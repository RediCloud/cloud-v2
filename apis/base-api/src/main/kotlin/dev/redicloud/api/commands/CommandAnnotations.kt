package dev.redicloud.api.commands

import kotlin.reflect.KClass

/**
 * This annotation is used to mark a class as a command and to specify the name of the command
 * @param name The name of the command
 */
@Target(AnnotationTarget.CLASS)
annotation class Command(val name: String)

/**
 * This annotation is used to mark a function as a sub command and to specify the path of the sub command.
 * Example: /test some test path <argument> -> path = "some test path <argument>"
 * If you want to set the default command, use an empty string as path.
 * @param path The path of the sub command
 */
@Target(AnnotationTarget.FUNCTION)
annotation class CommandSubPath(val path: String)

/**
 * This annotation can be used to specify aliases for a command or sub command path.
 * Use it like the {@link dev.redicloud.api.commands.Command} or {@link dev.redicloud.api.commands.CommandSubPath} annotation.
 * @param aliases The list of aliases
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class CommandAlias(val aliases: Array<String>)

/**
 * This annotation can be used to specify a command parameter in a command sub path function.
 * @param name The name of the parameter. It should be the same as the name that was defined in the sub path.
 * @param required If the parameter is required or not. If the parameter is not required, it will be automatically set to null if it is not set.
 * @param suggester The suggester that should be used for this parameter. The suggester must be a subclass of {@link dev.redicloud.api.commands.AbstractCommandSuggester}.
 * @param suggesterArguments The arguments can be used to specify arguments for the suggester (or custom command help {@see dev.redicloud.api.commands.CommandContext})
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class CommandParameter(val name: String = "", val required: Boolean = true, val suggester: KClass<out AbstractCommandSuggester> = EmptySuggester::class, val suggesterArguments: Array<String> = []) //TODO: required will be automatically set to false if the type is nullable

/**
 * This annotation can be used to specify the description of a command or sub command.
 * The description will be shown in the help command.
 * @param description The description of the command or sub command
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class CommandDescription(val description: String)

/**
 * This annotation can be used, if you want to hide a command or sub command from the help command and the tab completion.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class CommandInvisible

/**
 * This annotation can be used to specify the permission of a command or sub command.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class CommandPermission(val permission: String)