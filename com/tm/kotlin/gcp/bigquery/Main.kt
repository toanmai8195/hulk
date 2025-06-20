package com.tm.kotlin.gcp.bigquery

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.opencsv.CSVReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

val clientCredentials: ServiceAccountCredentials =
    ServiceAccountCredentials.fromStream(FileInputStream(getCredentialsFile("client-auth")))
val storage: Storage = StorageOptions.newBuilder().setCredentials(clientCredentials).build().service
const val bucketName = "momovn-dev"
const val folder = "helios/bp_broadcast/2025-02-25"
const val fileName = "$folder/test_2025_02_25_1.csv"

fun main() {
    readFile()
    readAllCsvFilesInFolder(folder)
}

fun getCredentialsFile(auth: String): File {
    val runfiles = System.getenv("RUNFILES_DIR") ?: System.getenv("JAVA_RUNFILES") ?: "."
    return File("$runfiles/_main/com/tm/kotlin/gcp/iam/$auth.json")
}


fun readFile() {
    val blob = storage.get(BlobId.of(bucketName, fileName))
    val reader = CSVReader(InputStreamReader(blob.getContent().inputStream()))
    reader.use {
        var line: Array<String>?
        while (reader.readNext().also { line = it } != null) {
            println(line?.joinToString(", "))
        }
    }
}


fun readAllCsvFilesInFolder(prefix: String) {
    val blobs = storage.list(bucketName, Storage.BlobListOption.prefix(prefix))

    for (blob in blobs.iterateAll()) {
        if (blob.name.endsWith(".csv")) {
            println("Reading file: ${blob.name}")
            val reader = CSVReader(InputStreamReader(blob.getContent().inputStream()))
            reader.use {
                var line: Array<String>?
                while (reader.readNext().also { line = it } != null) {
                    println(line?.joinToString(", "))
                }
            }
        }
    }
}
