package com.tm.kotlin.service.hbase_bulkload

/**
 * Configuration for HBase Bulk Load service
 */
data class BulkLoadConfig(
    val minioBucket: String,
    val minioEndpoint: String,
    val minioAccessKey: String,
    val minioSecretKey: String,
    val hbaseTable: String,
    val columnFamily: String,
    val hbaseZookeeperQuorum: String,
    val hbaseZookeeperPort: String,
    val hdfsUri: String
) {
    companion object {
        fun fromEnv(): BulkLoadConfig {
            return BulkLoadConfig(
                minioBucket = System.getenv("MINIO_BUCKET") ?: "hbase-bulkload",
                minioEndpoint = System.getenv("MINIO_ENDPOINT") ?: "http://localhost:9000",
                minioAccessKey = System.getenv("MINIO_ACCESS_KEY") ?: "minioadmin",
                minioSecretKey = System.getenv("MINIO_SECRET_KEY") ?: "minioadmin",
                hbaseTable = System.getenv("HBASE_TABLE") ?: "hulk:segment_index",
                columnFamily = System.getenv("HBASE_COLUMN_FAMILY") ?: "cf",
                hbaseZookeeperQuorum = System.getenv("HBASE_ZOOKEEPER_QUORUM") ?: "localhost",
                hbaseZookeeperPort = System.getenv("HBASE_ZOOKEEPER_PORT") ?: "2181",
                hdfsUri = System.getenv("HDFS_URI") ?: "//hbase-bulkload"
            )
        }
    }
}
