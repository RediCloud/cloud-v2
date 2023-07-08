package dev.redicloud.utils

import java.util.regex.Pattern

open class ProcessConfiguration(
    val jvmArguments: MutableList<String> = mutableListOf(),
    val environmentVariables: MutableMap<String, String> = mutableMapOf(),
    val programmParameters: MutableList<String> = mutableListOf(),
    // URL -> Path
    val defaultFiles: MutableMap<String, String> = mutableMapOf(),
    // Path -> (edit key -> edit value)
    val fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf(),
    var references: MutableList<ProcessConfiguration>? = mutableListOf()
) {

    fun getLibPatterns(vararg files: String): List<Pattern> {
        val patterns = mutableListOf<Pattern>()
        if (files.isNotEmpty()) {
            patterns.add(Pattern.compile("(${files.joinToString("|")})"))
        }
        if (defaultFiles.keys.isNotEmpty()) {
            patterns.add(Pattern.compile("(${defaultFiles.keys.joinToString("|")})"))
        }
        references?.forEach {
            patterns.addAll(it.getLibPatterns())
            if (it.defaultFiles.isNotEmpty()) {
                patterns.add(Pattern.compile("(${it.defaultFiles.keys.joinToString("|")})"))
            }
        }
        return patterns
    }

    companion object {
        fun collect(vararg processConfigurations: ProcessConfiguration): ProcessConfiguration {
            val collected = ProcessConfiguration()
            processConfigurations.forEach {
                collected.jvmArguments.addAll(it.jvmArguments)
                collected.environmentVariables.putAll(it.environmentVariables)
                collected.programmParameters.addAll(it.programmParameters)
                collected.defaultFiles.putAll(it.defaultFiles)
                collected.fileEdits.putAll(it.fileEdits)
                if (collected.references == null) collected.references = mutableListOf()
                collected.references!!.add(it)
            }
            return collected
        }
    }

}