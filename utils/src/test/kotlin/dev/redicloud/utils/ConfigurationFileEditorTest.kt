package dev.redicloud.utils

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull


class ConfigurationFileEditorTest : UtilTest() {

    private val exampleYml = """
            test: test-value
            test-123: test-123-value
    """.trimIndent()

    @Test
    fun ymlEdit() {
        val file = File("test.yml")
        try {
            file.createNewFile()
            file.writeText(exampleYml)
            val editor = ConfigurationFileEditor.ofFile(file)
            assertNotNull(editor, "Editor is null")
            editor.setValue("test", "new-value")
            editor.saveToFile(file)

            val newEditor = ConfigurationFileEditor.ofFile(file)
            assertNotNull(newEditor, "New editor is null")
            assert(newEditor.getValue("test") == "new-value")
            assert(newEditor.getValue("test-123") == "test-123-value")
        } finally {
            file.delete()
        }
    }

    private val exampleToml = """
            test = "test-value"
            test-2 = "test"
            test-123 = "test-123-value"
    """.trimIndent()

    @Test
    fun tomlEdit() {
        val file = File("test.toml")
        try {
            file.createNewFile()
            file.writeText(exampleToml)
            val editor = ConfigurationFileEditor.ofFile(file)
            assertNotNull(editor, "Editor is null")
            editor.setValue("test", "\"new-value\"")
            editor.setValue("test-2", "2")
            editor.saveToFile(file)

            val newEditor = ConfigurationFileEditor.ofFile(file)
            assertNotNull(newEditor, "New editor is null")
            assert(newEditor.getValue("test") == "\"new-value\"")
            assert(newEditor.getValue("test-2") == "2")
            assert(newEditor.getValue("test-123") == "\"test-123-value\"")
        } finally {
            file.delete()
        }
    }

    private val exampleProperties = """
            test=test-value
            test-123=test-123-value
            test-2=test-2-value
    """.trimIndent()

    @Test
    fun propertiesEdit() {
        val file = File("test.properties")
        try {
            file.createNewFile()
            file.writeText(exampleProperties)
            val editor = ConfigurationFileEditor.ofFile(file)
            assertNotNull(editor, "Editor is null")
            editor.setValue("test", "new-value")
            editor.setValue("test-2", "2")
            editor.saveToFile(file)

            val newEditor = ConfigurationFileEditor.ofFile(file)
            assertNotNull(newEditor, "New editor is null")
            assert(newEditor.getValue("test") == "new-value")
            assert(newEditor.getValue("test-123") == "test-123-value")
            assert(newEditor.getValue("test-2") == "2")
        } finally {
            file.delete()
        }
    }

}