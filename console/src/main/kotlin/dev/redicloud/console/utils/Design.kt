package dev.redicloud.console.utils

import dev.redicloud.utils.prettyPrintGson
import org.fusesource.jansi.Ansi

class Design(val foreground: Int? = null, val background: Int? = null, private val attributes: List<Ansi.Attribute> = listOf()) {

    constructor(foreground: Ansi.Color? = null, background: Ansi.Color? = null, attributes: List<Ansi.Attribute> = listOf())
            : this(foreground?.fg(), background?.bg(), attributes)

    fun apply(text: String): String {
        val ansi = Ansi.ansi()
        if (foreground != null) ansi.fg(foreground)
        if (background != null) ansi.bg(background)
        attributes.forEach { ansi.a(it) }
        return ansi.a(text).reset().toString()
    }

    fun toJson(): String = prettyPrintGson.toJson(this)

    companion object {
        fun fromJson(json: String): Design = prettyPrintGson.fromJson(json, Design::class.java)
    }
}