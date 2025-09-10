package com.tm.kotlin.backupdata

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val component = DaggerBackupDataComponent.create()
    val backupService = component.backupDataService()
    
    try {
        println("Initializing Backup Data Service...")
        backupService.init()
        
        println("Testing data retrieval...")
        
        // Test get single record
        val singleData = backupService.getData("test_row_1")
        println("Single record: $singleData")
        
        // Test scan multiple records
        val scanData = backupService.scanData("test_row_1", "test_row_10")
        println("Scan results: ${scanData.size} records")
        scanData.forEach { record ->
            println("  Row: ${record["rowKey"]}")
        }
        
        println("Backup Data Service is running successfully!")
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}