package com.tm.kotlin.service.testbitmap

import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.random.Random
import kotlin.system.measureTimeMillis

fun main() {
    println("🚀 RoaringBitmap Serialization Test")
    println("=" * 80)

    // Generate 50 million random 10-digit numbers
    val elementCount = 50_000_000
    val minValue = 1_000_000_000
    val maxValue = 2_147_483_647 // Max int value

    println("📊 Generating $elementCount random numbers (10 digits)...")

    val bitmap = RoaringBitmap()
    val generationTime = measureTimeMillis {
        val random = Random(System.currentTimeMillis())
        repeat(elementCount) {
            val randomNumber = random.nextInt(minValue, maxValue)
            bitmap.add(randomNumber)

            // Progress indicator
            if ((it + 1) % 5_000_000 == 0) {
                println("  Generated ${it + 1} numbers...")
            }
        }
    }

    println("\n✅ Generation completed!")
    println("   Time: ${generationTime}ms (${generationTime / 1000.0}s)")
    println("   Actual unique elements: ${bitmap.cardinality}")
    println("   Memory size (approximate): ${bitmap.getSizeInBytes()} bytes (${bitmap.getSizeInBytes() / 1024 / 1024} MB)")

    // Test serialization
    println("\n📤 Testing serialization...")
    var serializedBytes: ByteArray? = null
    val serializeTime = measureTimeMillis {
        val byteStream = ByteArrayOutputStream()
        val dataStream = DataOutputStream(byteStream)
        bitmap.serialize(dataStream)
        dataStream.flush()
        serializedBytes = byteStream.toByteArray()
    }

    println("✅ Serialization completed!")
    println("   Time: ${serializeTime}ms")
    println("   Serialized size: ${serializedBytes!!.size} bytes (${serializedBytes!!.size / 1024 / 1024} MB)")

    // Test deserialization
    println("\n📥 Testing deserialization...")
    var deserializedBitmap: RoaringBitmap? = null
    val deserializeTime = measureTimeMillis {
        val byteStream = ByteArrayInputStream(serializedBytes)
        val dataStream = DataInputStream(byteStream)
        deserializedBitmap = RoaringBitmap()
        deserializedBitmap!!.deserialize(dataStream)
    }

    println("✅ Deserialization completed!")
    println("   Time: ${deserializeTime}ms")
    println("   Deserialized elements: ${deserializedBitmap!!.cardinality}")

    // Verify correctness
    println("\n🔍 Verifying data integrity...")
    val isEqual = bitmap == deserializedBitmap
    println(if (isEqual) "   ✅ Data integrity verified!" else "   ❌ Data mismatch!")

    // Summary
    println("\n" + "=" * 80)
    println("📈 Performance Summary:")
    println("   Generation:      ${generationTime}ms (${generationTime / 1000.0}s)")
    println("   Serialization:   ${serializeTime}ms")
    println("   Deserialization: ${deserializeTime}ms")
    println("   Total:           ${generationTime + serializeTime + deserializeTime}ms")
    println("=" * 80)
}

operator fun String.times(n: Int): String = this.repeat(n)
