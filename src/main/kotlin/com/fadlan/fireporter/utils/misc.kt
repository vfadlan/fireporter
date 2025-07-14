package com.fadlan.fireporter.utils

import java.io.FileInputStream
import java.util.Properties

fun Any.prettyPrint() {

    var indentLevel = 0
    val indentWidth = 4

    fun padding() = "".padStart(indentLevel * indentWidth)

    val toString = toString()

    val stringBuilder = StringBuilder(toString.length)

    var i = 0
    while (i < toString.length) {
        when (val char = toString[i]) {
            '(', '[', '{' -> {
                indentLevel++
                stringBuilder.appendLine(char).append(padding())
            }
            ')', ']', '}' -> {
                indentLevel--
                stringBuilder.appendLine().append(padding()).append(char)
            }
            ',' -> {
                stringBuilder.appendLine(char).append(padding())
                // ignore space after comma as we have added a newline
                val nextChar = toString.getOrElse(i + 1) { char }
                if (nextChar == ' ') i++
            }
            else -> {
                stringBuilder.append(char)
            }
        }
        i++
    }

    println(stringBuilder.toString())
}

fun loadProperties(path: String): Properties? {
    val safePath = if (!path.startsWith("/") || !path.startsWith("\\")) "/$path"
                else path
    val properties = object {}.javaClass
        .getResourceAsStream(safePath)
        ?.use { stream ->
            java.util.Properties().apply { load(stream) }
        }
    return properties
}

fun getProperty(path: String, key: String): String {
    val property = loadProperties(path)
    return property?.getProperty(key) ?: "Unknown"
}