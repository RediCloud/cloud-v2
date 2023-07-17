package dev.redicloud.commands.api

import dev.redicloud.api.commands.ICommandArgumentParser
import kotlin.reflect.KClass

val PARSERS = mutableMapOf<KClass<*>, ICommandArgumentParser<*>>(
    String::class to StringCommandArgumentParser(),
    Int::class to IntCommandArgumentParser(),
    Long::class to LongCommandArgumentParser(),
    Double::class to DoubleCommandArgumentParser(),
    Float::class to FloatCommandArgumentParser(),
    Boolean::class to BooleanCommandArgumentParser(),
    Byte::class to ByteCommandArgumentParser(),
    Short::class to ShortCommandArgumentParser(),
    Char::class to CharCommandArgumentParser()
)

class StringCommandArgumentParser : ICommandArgumentParser<String> {
    override fun parse(parameter: String): String = parameter
}

class IntCommandArgumentParser : ICommandArgumentParser<Int> {
    override fun parse(parameter: String): Int? = parameter.toIntOrNull()
}

class LongCommandArgumentParser : ICommandArgumentParser<Long> {
    override fun parse(parameter: String): Long? = parameter.toLongOrNull()
}

class DoubleCommandArgumentParser : ICommandArgumentParser<Double> {
    override fun parse(parameter: String): Double? = parameter.toDoubleOrNull()
}

class FloatCommandArgumentParser : ICommandArgumentParser<Float> {
    override fun parse(parameter: String): Float? = parameter.toFloatOrNull()
}

class BooleanCommandArgumentParser : ICommandArgumentParser<Boolean> {
    override fun parse(parameter: String): Boolean? {
        return when (parameter.lowercase()) {
            "true" -> true
            "yes" -> true
            "y" -> true
            "false" -> false
            "no" -> false
            "n" -> false
            else -> null
        }
    }
}

class ByteCommandArgumentParser : ICommandArgumentParser<Byte> {
    override fun parse(parameter: String): Byte? = parameter.toByteOrNull()
}

class ShortCommandArgumentParser : ICommandArgumentParser<Short> {
    override fun parse(parameter: String): Short? = parameter.toShortOrNull()
}

class CharCommandArgumentParser : ICommandArgumentParser<Char> {
    override fun parse(parameter: String): Char? = parameter.firstOrNull()
}