package dev.redicloud.commands.api

import java.lang.reflect.Parameter
import kotlin.reflect.KClass

class CommandArgument(commandSubBase: CommandSubBase, parameter: Parameter, val index: Int) {

    val name: String
    val required: Boolean //TODO
    val clazz: KClass<*>
    val parser: CommandParser<*>

    init {
        if (!parameter.isAnnotationPresent(CommandParameter::class.java)) {
            name = parameter.name
            required = !parameter.isImplicit //TODO check String? and Int? etc.
        } else {
            val annotation = parameter.getAnnotation(CommandParameter::class.java)
            name = annotation.name.ifEmpty { parameter.name }
            required = annotation.required //TODO check String? and Int? etc.
        }
        clazz = parameter.type.kotlin
        parser = CommandParser.PARSERS.filter {
            it.key.qualifiedName!!.replace("?", "") == clazz.qualifiedName!!.replace("?", "")
        }.values.first()
    }

    fun getPathFormat(): String = if (required) "<$name>" else "[$name]"

}