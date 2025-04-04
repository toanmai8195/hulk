package com.tm.kotlin

import java.security.MessageDigest

class Main {
}

fun main() {
    val a = hash32Chars(System.currentTimeMillis().toString())
    println(a)
    println(hash32Chars("01666621555#59911080#$a"))
}


fun hash32Chars(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }.substring(0, 32)
}