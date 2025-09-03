package com.tm.kotlin.service.ws

import java.io.File
import java.io.FileWriter

fun main() {
    val path = "example.txt"
    writeToFile(path, listOf("Hello", "World"))
    readLargeFileLineByLine(path)
}

fun readLargeFileLineByLine(filePath: String) {
    File(filePath).bufferedReader().useLines { lines ->
        lines.forEach { line ->
            println(line)
        }
    }
}

fun writeToFile(filePath: String, lines: List<String>, append: Boolean = false) {
    FileWriter(filePath, append).buffered().use { writer ->
        lines.forEach { line ->
            writer.write(line)
            writer.newLine()
        }
    }
}