package com.tm.kotlin.service.coroutine.httpserver.hbase

import org.apache.hadoop.hbase.util.Bytes
import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayInputStream
import java.io.DataInputStream

object HBaseUtils {
    fun ByteArray.toBitmap(): RoaringBitmap {
        val byteArrayInputStream = ByteArrayInputStream(this)
        val dataInputStream = DataInputStream(byteArrayInputStream)
        val bitmap = RoaringBitmap()
        bitmap.deserialize(dataInputStream)
        return bitmap
    }

    fun String.getRow(): String = this.reversed()

    fun ByteArray.toStringValue(): String = Bytes.toString(this)
}