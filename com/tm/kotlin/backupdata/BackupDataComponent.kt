package com.tm.kotlin.backupdata

import com.tm.kotlin.common.hbase.HBaseClientFactory
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        BackupDataModule::class,
        HBaseClientFactory::class
    ]
)
interface BackupDataComponent {
    fun backupDataService(): BackupDataService
}