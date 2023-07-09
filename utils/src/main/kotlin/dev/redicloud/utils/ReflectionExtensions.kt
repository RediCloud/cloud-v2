package dev.redicloud.utils

import java.io.File


fun getClassesWithPrefix(packagePrefix: String): List<Class<*>> {
    val classLoader = Thread.currentThread().contextClassLoader
    val packageName = packagePrefix.substringBeforeLast(".")
    val packagePath = packageName.replace('.', '/')
    val resources = classLoader.getResources(packagePath)
    val classes = mutableListOf<Class<*>>()

    while (resources.hasMoreElements()) {
        val resource = resources.nextElement()
        val file = File(resource.file)
        findClasses(file, packagePrefix, classes)
    }

    return classes
}

fun findClasses(file: File, packageName: String, classes: MutableList<Class<*>>) {
    if (file.isDirectory) {
        val files = file.listFiles()
        if (files != null) {
            for (subFile in files) {
                findClasses(subFile, packageName, classes)
            }
        }
    } else if (file.name.endsWith(".class")) {
        val className = packageName + '.' + file.name.substring(0, file.name.length - 6)
        try {
            val clazz = Class.forName(className)
            if (className.startsWith(packageName)) {
                classes.add(clazz)
            }
        } catch (e: ClassNotFoundException) {
            // Fehler beim Laden der Klasse
        }
    }
}