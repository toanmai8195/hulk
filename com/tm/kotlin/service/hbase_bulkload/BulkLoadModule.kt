package com.tm.kotlin.service.hbase_bulkload

import dagger.Module
import dagger.Provides
import io.minio.MinioClient
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import javax.inject.Singleton

/**
 * Dagger module for dependency injection
 */
@Module
class BulkLoadModule {

    @Provides
    @Singleton
    fun provideConfig(): BulkLoadConfig {
        return BulkLoadConfig.fromEnv()
    }

    @Provides
    @Singleton
    fun provideMinioClient(config: BulkLoadConfig): MinioClient {
        return MinioClient.builder()
            .endpoint(config.minioEndpoint)
            .credentials(config.minioAccessKey, config.minioSecretKey)
            .build()
    }

    @Provides
    @Singleton
    fun provideHBaseConfiguration(config: BulkLoadConfig): Configuration {
        val hbaseConfig = HBaseConfiguration.create()
        hbaseConfig.set("hbase.zookeeper.quorum", config.hbaseZookeeperQuorum)
        hbaseConfig.set("hbase.zookeeper.property.clientPort", config.hbaseZookeeperPort)

        // Hadoop filesystem config
        hbaseConfig.set("fs.defaultFS", config.hdfsUri)
        hbaseConfig.set("hadoop.native.lib", "false")
        hbaseConfig.set("dfs.permissions.enabled", "false")
        hbaseConfig.set("fs.permissions.umask-mode", "000")
        hbaseConfig.setBoolean("dfs.support.append", true)

        return hbaseConfig
    }
}
