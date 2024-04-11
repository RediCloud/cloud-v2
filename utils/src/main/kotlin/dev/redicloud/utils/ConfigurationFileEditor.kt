package dev.redicloud.utils

import java.io.File
import java.nio.charset.StandardCharsets

class ConfigurationFileEditor private constructor(
    private val linesWithSpaces: List<String>,
    private val keyValueSplitter: String
){

    companion object {
        const val YAML_SPLITTER = ": "
        const val PROPERTIES_SPLITTER = "="
        const val TOML_SPLITTER = " = "
        fun ofFile(file: File): ConfigurationFileEditor? {
            val keyValueSplitter = when(file.extension) {
                "yml" -> YAML_SPLITTER
                "properties" -> PROPERTIES_SPLITTER
                "toml" -> TOML_SPLITTER
                "secret" -> ""
                else -> return null
            }
            return ConfigurationFileEditor(file.readLines(StandardCharsets.UTF_8), keyValueSplitter)
        }
    }

    private val lines = this.linesWithSpaces.map { removeFirstSpaces(it) }

    private val keyToValues = HashMap(getKeyToValueMapByLines(this.lines))

    private fun getKeyToValueMapByLines(lines: List<String>): Map<String, String> {
        val keyValueSplitArrays = lines.filter { it.contains(keyValueSplitter) }.map { it.split(keyValueSplitter) }
        return keyValueSplitArrays.associate { it[0] to (it.getOrNull(1) ?: "") }
    }

    fun getValue(key: String): String? {
        return this.keyToValues[key]
    }

    fun setValue(key: String, value: String) {
        if (!this.keyToValues.containsKey(key)) throw IllegalStateException("Key '${key}' does not exist")
        this.keyToValues[key] = value
    }

    fun saveToFile(file: File) {
        val linesToSave = generateNewLines()
        file.writeText(linesToSave.joinToString("\n"))
    }

    private fun generateNewLines(): List<String> {
        val mutableLines = ArrayList(this.linesWithSpaces)
        for ((key, value) in this.keyToValues) {
            val lineIndex = getLineIndexByKey(key)
            val newLine = constructNewLine(key, value, lineIndex)
            mutableLines[lineIndex] = newLine
        }
        return mutableLines
    }

    private fun constructNewLine(key: String, value: String, lineIndex: Int): String {
        val lineWithoutSpaces = key + this.keyValueSplitter + value
        val amountOfStartSpaces = getAmountOfStartSpacesInLine(this.linesWithSpaces[lineIndex])
        val spacesString = getStringWithSpaces(amountOfStartSpaces)
        return spacesString + lineWithoutSpaces
    }

    private fun getStringWithSpaces(amount: Int): String {
        return (0 until amount).joinToString("") { " " }
    }

    private fun removeFirstSpaces(string: String): String {
        val amountOfSpaces = getAmountOfStartSpacesInLine(string)
        return string.drop(amountOfSpaces)
    }

    private fun getAmountOfStartSpacesInLine(line: String): Int {
        var s = line
        var amountOfSPaces = 0
        while (s.startsWith(" ")) {
            s = s.drop(1)
            amountOfSPaces++
        }
        return amountOfSPaces
    }

    private fun getLineIndexByKey(key: String): Int {
        val lineStart = key + this.keyValueSplitter
        val line = lines.lastOrNull { it.startsWith(lineStart) } ?: return -1
        return this.lines.indexOf(line)
    }


}