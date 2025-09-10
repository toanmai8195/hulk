package com.tm.kotlin.backupdata

import dagger.Module
import dagger.Provides
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import javax.inject.Singleton

@Module
class BackupDataModule {
    
    @Provides
    @Singleton
    fun provideHBaseConfiguration(): Configuration {
        val config = HBaseConfiguration.create()
        config.set("hbase.zookeeper.quorum", "localhost")
        config.set("hbase.zookeeper.property.clientPort", "2182")
        config.set("hbase.master", "localhost:16000")
        return config
    }
}