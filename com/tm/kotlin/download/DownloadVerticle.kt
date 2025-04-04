package com.tm.kotlin.download

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.core.buffer.Buffer
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DownloadVerticle : AbstractVerticle() {
    override fun start(startPromise: Promise<Void>) {
        val router = Router.router(vertx)

        router.get("/download").handler(this::handleDownloadCsv)
        router.get("/download-zip").handler(this::handleDownloadZip)

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080) { http ->
                if (http.succeeded()) {
                    startPromise.complete()
                    println("🚀 Server started on port 8080")
                } else {
                    startPromise.fail(http.cause())
                }
            }
    }

    private fun handleDownloadCsv(ctx: RoutingContext) {
        val csvData = generateCsv()

        // Sử dụng Buffer để gửi dữ liệu
        ctx.response()
            .putHeader("Content-Type", "text/csv")
            .putHeader("Content-Disposition", "attachment; filename=phone_numbers.csv")
            .end(Buffer.buffer(csvData)) // Chuyển đổi string thành Buffer
    }

    private fun handleDownloadZip(ctx: RoutingContext) {
        val zipData = generateZip()

        // Sử dụng Buffer để gửi dữ liệu
        ctx.response()
            .putHeader("Content-Type", "application/zip")
            .putHeader("Content-Disposition", "attachment; filename=phone_numbers.zip")
            .end(Buffer.buffer(zipData)) // Chuyển đổi byte array thành Buffer
    }

    private fun generateCsv(): String {
        val phoneNumbers = List(2_000_000) { generateVietnamPhone() }

        val outputStream = ByteArrayOutputStream()
        BufferedWriter(OutputStreamWriter(outputStream)).use { out ->
            out.write("Phone Number\n") // Header
            phoneNumbers.forEach { out.write("$it\n") }
        }
        return outputStream.toString("UTF-8")
    }

    private fun generateVietnamPhone(): String {
        val prefixes = listOf("03", "05", "07", "08", "09")
        val prefix = prefixes.random()
        val number = (1000000..9999999).random() // Đảm bảo 7 số còn lại
        return "$prefix$number"
    }

    private fun generateZip(): ByteArray {
        val phoneNumbers = List(2_000_000) { generateVietnamPhone() }

        val zipOutputStream = ByteArrayOutputStream()
        ZipOutputStream(zipOutputStream).use { zos ->
            val zipEntry = ZipEntry("phone_numbers.csv")
            zos.putNextEntry(zipEntry)

            // Tạo CSV trong bộ nhớ và nạp vào zip
            val csvData = phoneNumbers.joinToString("\n", "Phone Number\n") { it }
            zos.write(csvData.toByteArray())
            zos.closeEntry()
        }
        return zipOutputStream.toByteArray()
    }
}