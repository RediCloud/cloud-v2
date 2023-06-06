package dev.redicloud.commands.api

import kotlin.reflect.KClass


interface CommandArgumentParser<T> {

    companion object {
        val PARSERS = mutableMapOf<KClass<*>, CommandArgumentParser<*>>(
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
    }

    fun parse(parameter: String): T?
}

class StringCommandArgumentParser : CommandArgumentParser<String> {
    override fun parse(parameter: String): String = parameter
}

class IntCommandArgumentParser : CommandArgumentParser<Int> {
    override fun parse(parameter: String): Int? = parameter.toIntOrNull()
}

class LongCommandArgumentParser : CommandArgumentParser<Long> {
    override fun parse(parameter: String): Long? = parameter.toLongOrNull()
}

class DoubleCommandArgumentParser : CommandArgumentParser<Double> {
    override fun parse(parameter: String): Double? = parameter.toDoubleOrNull()
}

class FloatCommandArgumentParser : CommandArgumentParser<Float> {
    override fun parse(parameter: String): Float? = parameter.toFloatOrNull()
}

class BooleanCommandArgumentParser : CommandArgumentParser<Boolean> {
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

class ByteCommandArgumentParser : CommandArgumentParser<Byte> {
    override fun parse(parameter: String): Byte? = parameter.toByteOrNull()
}

class ShortCommandArgumentParser : CommandArgumentParser<Short> {
    override fun parse(parameter: String): Short? = parameter.toShortOrNull()
}

class CharCommandArgumentParser : CommandArgumentParser<Char> {
    override fun parse(parameter: String): Char? = parameter.firstOrNull()
}