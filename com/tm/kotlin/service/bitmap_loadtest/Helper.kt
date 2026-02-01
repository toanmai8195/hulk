package com.tm.kotlin.service.bitmap_loadtest

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Helper {
    const val POOL_SIZE = 2;

    // =========================================================
    // Resolve file path inside Bazel runfiles or Docker
    // =========================================================
    private fun getFilePath(): Path {
        // Docker path
        val dockerPath = Paths.get("/com/tm/kotlin/service/bitmap_loadtest/data/segment50M.bin")
        if (Files.exists(dockerPath)) {
            return dockerPath
        }

        // Bazel runfiles path
        val runfiles = System.getenv("RUNFILES_DIR")
            ?: System.getenv("JAVA_RUNFILES")
            ?: "."

        return Paths.get(
            runfiles,
            "_main/com/tm/kotlin/service/bitmap_loadtest/data/segment50M.bin"
        )
    }

    // =========================================================
    // READ byte[] FROM FILE + timing
    // =========================================================
    internal fun readBytesFromFile(): ByteArray {
        try {
            val t1 = System.currentTimeMillis()

            val path = getFilePath()
            val bytes = Files.readAllBytes(path)

            val t2 = System.currentTimeMillis()
            println(
                "Read ${bytes.size} bytes from ${path.toAbsolutePath()} in ${t2 - t1} ms"
            )

            return bytes
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}