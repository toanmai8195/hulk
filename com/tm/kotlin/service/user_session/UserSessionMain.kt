package com.tm.kotlin.service.user_session

import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray()
private val patternSize = BASE62_ALPHABET.size.toLong()
private val BASE62_REVERSE = BASE62_ALPHABET.withIndex().associate { it.value to it.index }
val key = "toanquat12345678".toByteArray()

fun main() {
    val userId = 811995
    val timestamp = System.currentTimeMillis() / 1000

    val sessionId = encodeSessionId(userId, timestamp)
    println("SessionID=$sessionId")

    val (decodeUserId, timeKey) = decodeSessionId(sessionId)
    println("UserId=$decodeUserId (timeKey=$timeKey)")
}

fun encodeSessionId(userId: Int, timestamp: Long = System.currentTimeMillis() / 1000): String {
    val timeKey = (timestamp / 3600).toInt()

    val buffer = ByteBuffer.allocate(8)
    buffer.putInt(userId)
    buffer.putInt(timeKey)
    val data = buffer.array()

    val secretKey = SecretKeySpec(key, "AES")

    val iv = ByteArray(16)
    SecureRandom().nextBytes(iv)
    val ivSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance("AES/CFB/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
    val encrypted = cipher.doFinal(data)

    val full = iv + encrypted
    return base62Encode(full)
}

fun decodeSessionId(sessionId: String): Pair<Int, Int> {
    val full = base62Decode(sessionId)

    if (full.size < 16 + 8) throw IllegalArgumentException("Invalid sessionId")

    val iv = full.copyOfRange(0, 16)
    val encrypted = full.copyOfRange(16, full.size)

    val cipher = Cipher.getInstance("AES/CFB/NoPadding")
    val secretKey = SecretKeySpec(key, "AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
    val decrypted = cipher.doFinal(encrypted)

    val buffer = ByteBuffer.wrap(decrypted)
    val userId = buffer.int
    val timeKey = buffer.int

    return userId to timeKey
}

fun base62Encode(input: ByteArray): String {
    var num = input.fold(0.toBigInteger()) { acc, byte ->
        (acc shl 8) or (byte.toInt() and 0xFF).toBigInteger()
    }

    val result = StringBuilder()
    while (num > BigInteger.ZERO) {
        val rem = num % BigInteger.valueOf(patternSize)
        result.append(BASE62_ALPHABET[rem.toInt()])
        num /= BigInteger.valueOf(patternSize)
    }

    return result.reverse().toString()
}

fun base62Decode(input: String): ByteArray {
    var num = BigInteger.ZERO
    for (char in input) {
        val value = BASE62_REVERSE[char] ?: error("Invalid character in Base62")
        num = num * BigInteger.valueOf(patternSize) + value.toBigInteger()
    }

    val byteList = mutableListOf<Byte>()
    while (num > BigInteger.ZERO) {
        byteList.add((num and BigInteger.valueOf(0xFF)).toByte())
        num = num shr 8
    }

    return byteList.reversed().toByteArray()
}