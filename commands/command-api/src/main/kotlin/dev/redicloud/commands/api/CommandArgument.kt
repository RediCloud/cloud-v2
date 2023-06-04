package dev.redicloud.commands.api

import java.lang.reflect.Parameter
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

class CommandArgument(commandSubBase: CommandSubBase, parameter: Parameter, val index: Int) {

    val name: String
    val required: Boolean //TODO
    val clazz: KClass<*>
    val parser: CommandArgumentParser<*>?
    val actorArgument: Boolean
    val suggester: ICommandSuggester

    init {
        if (parameter.type.kotlin.superclasses.any { it == ICommandActor::class }) {
            name = "_actor"
            required = true
            clazz = parameter.type.kotlin
            parser = null
            actorArgument = true
            suggester = EmptySuggester()
        }else {
            actorArgument = false
            if (!parameter.isAnnotationPresent(CommandParameter::class.java)) {
                name = parameter.name
                required = !parameter.isImplicit //TODO check String? and Int? etc.
                suggester = ICommandSuggester.SUGGESTERS.firstOrNull { it::class == parameter.type.kotlin } ?: EmptySuggester()
            } else {
                val annotation = parameter.getAnnotation(CommandParameter::class.java)
                name = annotation.name.ifEmpty { parameter.name }
                required = annotation.required //TODO check String? and Int? etc.
                suggester = EmptySuggester()
            }
            clazz = parameter.type.kotlin
            parser = CommandArgumentParser.PARSERS.filter {
                it.key.qualifiedName!!.replace("?", "") == clazz.qualifiedName!!.replace("?", "")
            }.values.first()
        }
    }

    fun parse(input: String): Any? = parser?.parse(input)

    fun getPathFormat(): String = if (required) "<$name>" else "[$name]"

}