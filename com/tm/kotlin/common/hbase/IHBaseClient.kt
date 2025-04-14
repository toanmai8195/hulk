package com.tm.kotlin.common.hbase

import org.apache.hadoop.hbase.client.Delete
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Increment
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.ResultScanner
import org.apache.hadoop.hbase.client.Scan

interface IHBaseClient {
    fun put(put: Put)

    fun get(get: Get): Result

    fun scan(scan: Scan): ResultScanner

    fun delete(delete: Delete)

    fun inc(increment: Increment): Result
}