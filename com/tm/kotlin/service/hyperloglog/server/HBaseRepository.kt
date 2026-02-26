package com.tm.kotlin.service.hyperloglog.server

import com.tm.kotlin.common.hbase.IHBaseClient
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.LoggerFactory

class HBaseRepository(
    private val hbaseClient: IHBaseClient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(HBaseRepository::class.java)
        private val COLUMN_FAMILY = Bytes.toBytes("hll")
        private val COLUMN_DATA = Bytes.toBytes("data")
    }

    fun save(timeBucket: String, hllBytes: ByteArray) {
        try {
            val put = Put(Bytes.toBytes(timeBucket))
            put.addColumn(COLUMN_FAMILY, COLUMN_DATA, hllBytes)
            hbaseClient.put(put)
            logger.info("Saved HLL for bucket: $timeBucket, size: ${hllBytes.size} bytes")
        } catch (e: Exception) {
            logger.error("Failed to save HLL for bucket: $timeBucket", e)
            throw e
        }
    }

    fun saveAll(hllData: Map<String, ByteArray>) {
        hllData.forEach { (bucket, bytes) ->
            save(bucket, bytes)
        }
        logger.info("Saved ${hllData.size} HLL buckets to HBase")
    }

    fun load(timeBucket: String): ByteArray? {
        return try {
            val get = Get(Bytes.toBytes(timeBucket))
            get.addColumn(COLUMN_FAMILY, COLUMN_DATA)
            val result = hbaseClient.get(get)
            if (result.isEmpty) {
                null
            } else {
                result.getValue(COLUMN_FAMILY, COLUMN_DATA)
            }
        } catch (e: Exception) {
            logger.error("Failed to load HLL for bucket: $timeBucket", e)
            null
        }
    }

    fun loadByPrefix(prefix: String): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        try {
            val scan = Scan()
            scan.setRowPrefixFilter(Bytes.toBytes(prefix))
            scan.addColumn(COLUMN_FAMILY, COLUMN_DATA)

            val scanner = hbaseClient.scan(scan)
            scanner.use { s ->
                for (r in s) {
                    val rowKey = Bytes.toString(r.row)
                    val data = r.getValue(COLUMN_FAMILY, COLUMN_DATA)
                    if (data != null) {
                        result[rowKey] = data
                    }
                }
            }
            logger.info("Loaded ${result.size} HLL buckets with prefix: $prefix")
        } catch (e: Exception) {
            logger.error("Failed to load HLL with prefix: $prefix", e)
        }
        return result
    }

    fun loadCurrentBuckets(dailyBucket: String, hourlyBucket: String): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()

        load(dailyBucket)?.let { result[dailyBucket] = it }
        load(hourlyBucket)?.let { result[hourlyBucket] = it }

        logger.info("Loaded ${result.size} current HLL buckets from HBase")
        return result
    }
}
