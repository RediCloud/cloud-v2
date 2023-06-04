package dev.redicloud.commands.api

import kotlin.reflect.KClass


interface CommandParser<T> {

    companion object {
        val PARSERS = mutableMapOf<KClass<*>, CommandParser<*>>(
            String::class to StringCommandParser(),
            Int::class to IntCommandParser(),
            Long::class to LongCommandParser(),
            Double::class to DoubleCommandParser(),
            Float::class to FloatCommandParser(),
            Boolean::class to BooleanCommandParser(),
            Byte::class to ByteCommandParser(),
            Short::class to ShortCommandParser(),
            Char::class to CharCommandParser()
        )
    }

    fun parse(parameter: String): T?
}

class StringCommandParser : CommandParser<String> {
    override fun parse(parameter: String): String = parameter
}

class IntCommandParser : CommandParser<Int> {
    override fun parse(parameter: String): Int? = parameter.toIntOrNull()
}

class LongCommandParser : CommandParser<Long> {
    override fun parse(parameter: String): Long? = parameter.toLongOrNull()
}

class DoubleCommandParser : CommandParser<Double> {
    override fun parse(parameter: String): Double? = parameter.toDoubleOrNull()
}

class FloatCommandParser : CommandParser<Float> {
    override fun parse(parameter: String): Float? = parameter.toFloatOrNull()
}

class BooleanCommandParser : CommandParser<Boolean> {
    override fun parse(parameter: String): Boolean? = parameter.toBooleanStrictOrNull()
}

class ByteCommandParser : CommandParser<Byte> {
    override fun parse(parameter: String): Byte? = parameter.toByteOrNull()
}

class ShortCommandParser : CommandParser<Short> {
    override fun parse(parameter: String): Short? = parameter.toShortOrNull()
}

class CharCommandParser : CommandParser<Char> {
    override fun parse(parameter: String): Char? = parameter.firstOrNull()
}