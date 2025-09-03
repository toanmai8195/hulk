package com.tm.kotlin.service.user_session

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

object Base62 {
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val BASE = ALPHABET.length

    fun encode(bytes: ByteArray): String {
        var num = java.math.BigInteger(1, bytes)
        val sb = StringBuilder()
        while (num > java.math.BigInteger.ZERO) {
            val divRem = num.divideAndRemainder(java.math.BigInteger.valueOf(BASE.toLong()))
            sb.append(ALPHABET[divRem[1].toInt()])
            num = divRem[0]
        }
        return sb.reverse().toString()
    }

    fun decode(str: String): ByteArray {
        var num = java.math.BigInteger.ZERO
        for (c in str) {
            num = num.multiply(java.math.BigInteger.valueOf(BASE.toLong()))
                .add(java.math.BigInteger.valueOf(ALPHABET.indexOf(c).toLong()))
        }
        val bytes = num.toByteArray()
        return if (bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
    }
}

/**
 * RelationshipID encoder/decoder producing a fixed-length 40-char Base64URL token (30 raw bytes).
 *
 * Layout (42 bytes):
 * [0]      : version (1)
 * [1]      : userIdLen (9..11)
 * [2..12]  : userId bytes (padded with 0 to 11 bytes)
 * [13]     : actorIdLen (9..11)
 * [14..24] : actorId bytes (padded with 0 to 11 bytes)
 * [25..26] : permission bitmask (16 bits, big-endian)
 * [27]     : reserved (0)
 * [28..31] : nonce (4 random bytes)
 * [32..41] : MAC truncation (first 10 bytes of HMAC-SHA256 over bytes[0..31])
 *
 * Base64 URL-safe (no padding) of 42 bytes => exactly 56 characters.
 */
class RelationshipIDMain {

    data class RelationshipPayload(
        val userId: String,
        val permissions: Set<Permission>,
        val actorId: String?
    )

    enum class Permission {
        PROFILE,
        TRANSACTION,
        CHAT_HISTORY,
        FRIENDS,
        BALANCE,
        STATEMENT,
        NOTIFICATIONS,
        SETTINGS,
        EMAIL,
        PHONE,
        ADDRESS,
        ORDERS,
        CARDS,
        KYC,
        DEVICES,
        SECURITY;

        companion object {
            fun fromStrings(values: Collection<String>): Set<Permission> =
                values.mapNotNull { runCatching { valueOf(it.uppercase()) }.getOrNull() }.toSet()
        }
    }

    companion object {
        private const val VERSION: Byte = 1
        private const val RAW_SIZE = 42 // bytes -> 56 Base64URL chars (no padding)
        private const val USER_MIN = 9
        private const val USER_MAX = 11
        private const val USER_SLOT = 11 // reserved slot for user bytes inside token
        private const val ACTOR_MIN = 9
        private const val ACTOR_MAX = 11
        private const val ACTOR_SLOT = 11
        private const val HMAC_ALGO = "HmacSHA256"
        private const val SECRET = "toanmai1" // move to secure config/ENV in production

        private val rnd = SecureRandom()

        /**
         * Encode a token from userId and permissions.
         * Returns a 56-character Base64URL string.
         */
        @JvmStatic
        fun encode(userId: String, permissions: Collection<String>): String {
            val perms = Permission.fromStrings(permissions)
            return encode(userId, perms)
        }

        /**
         * Overload for type-safe Permission set.
         */
        @JvmStatic
        fun encode(userId: String, permissions: Set<Permission>, actorId: String? = null): String {
            require(userId.isNotBlank()) { "userId is blank" }
            val userBytes = userId.toByteArray(UTF_8)
            require(userBytes.size in USER_MIN..USER_MAX) { "userId must be $USER_MIN..$USER_MAX bytes (UTF-8)" }

            val actorBytes = actorId?.toByteArray(UTF_8) ?: ByteArray(0)
            if (actorId != null) {
                require(actorBytes.size in ACTOR_MIN..ACTOR_MAX) { "actorId must be $ACTOR_MIN..$ACTOR_MAX bytes (UTF-8)" }
            }

            val buf = ByteArray(RAW_SIZE)
            var i = 0
            buf[i++] = VERSION
            buf[i++] = userBytes.size.toByte()

            // user bytes (padded to USER_SLOT with 0)
            System.arraycopy(userBytes, 0, buf, i, userBytes.size)
            for (p in userBytes.size until USER_SLOT) buf[i + p] = 0
            i += USER_SLOT

            // actorId length and bytes (padded to ACTOR_SLOT with 0)
            if (actorId != null) {
                buf[i++] = actorBytes.size.toByte()
                System.arraycopy(actorBytes, 0, buf, i, actorBytes.size)
                for (p in actorBytes.size until ACTOR_SLOT) buf[i + p] = 0
                i += ACTOR_SLOT
            } else {
                buf[i++] = 0
                for (p in 0 until ACTOR_SLOT) buf[i + p] = 0
                i += ACTOR_SLOT
            }

            // permissions -> 16-bit mask (big-endian)
            val mask = toBitMask(permissions)
            buf[i++] = ((mask ushr 8) and 0xFF).toByte()
            buf[i++] = (mask and 0xFF).toByte()

            // reserved
            buf[i++] = 0

            // nonce (4 bytes)
            val nonce = ByteArray(4).also { rnd.nextBytes(it) }
            System.arraycopy(nonce, 0, buf, i, 4)
            i += 4

            // MAC over header/body (first 32 bytes)
            val macFull = hmac(buf, 0, 32)
            System.arraycopy(macFull, 0, buf, i, 10) // truncate to 10 bytes

            // Base64 URL-safe no padding -> 56 chars
            return Base62.encode(buf)
        }

        /**
         * Decode token and verify MAC. Throws IllegalArgumentException on invalid input.
         */
        @JvmStatic
        fun decode(token: String): RelationshipPayload {
            val raw = try {
                Base62.decode(token)
            } catch (e: Exception) {
                throw IllegalArgumentException("Token is not valid Base62", e)
            }
            require(raw.size == RAW_SIZE) { "Token must decode to $RAW_SIZE bytes" }

            // verify version
            require(raw[0] == VERSION) { "Unsupported version" }

            // verify MAC
            val expected = hmac(raw, 0, 32)
            for (k in 0 until 10) {
                if (raw[32 + k] != expected[k]) throw IllegalArgumentException("Invalid signature")
            }

            val userLen = (raw[1].toInt() and 0xFF)
            require(userLen in USER_MIN..USER_MAX) { "userId length invalid" }

            val userBytes = raw.copyOfRange(2, 2 + userLen)
            val userId = userBytes.toString(UTF_8)

            val actorLen = (raw[13].toInt() and 0xFF)
            val actorId = if (actorLen in ACTOR_MIN..ACTOR_MAX) {
                val actorBytes = raw.copyOfRange(14, 14 + actorLen)
                actorBytes.toString(UTF_8)
            } else {
                null
            }

            val permHi = (raw[25].toInt() and 0xFF)
            val permLo = (raw[26].toInt() and 0xFF)
            val mask = (permHi shl 8) or permLo
            val perms = fromBitMask(mask)

            return RelationshipPayload(userId, perms, actorId)
        }

        private fun toBitMask(perms: Set<Permission>): Int {
            var mask = 0
            perms.forEach { p ->
                val bit = p.ordinal
                if (bit < 16) mask = mask or (1 shl bit)
            }
            return mask
        }

        private fun fromBitMask(mask: Int): Set<Permission> {
            val out = mutableSetOf<Permission>()
            Permission.entries.forEach { p ->
                val bit = p.ordinal
                if (bit < 16 && (mask and (1 shl bit)) != 0) out += p
            }
            return out
        }

        private fun hmac(data: ByteArray, offset: Int, len: Int): ByteArray {
            val mac = Mac.getInstance(HMAC_ALGO)
            mac.init(SecretKeySpec(SECRET.toByteArray(UTF_8), HMAC_ALGO))
            mac.update(data, offset, len)
            return mac.doFinal() // 32 bytes; caller truncates as needed
        }
    }
}

fun main() {
    val permissions = setOf(
        RelationshipIDMain.Permission.PROFILE,
        RelationshipIDMain.Permission.CHAT_HISTORY,
        RelationshipIDMain.Permission.TRANSACTION
    )

    // Encode a single userId and print token
    val userId = "100000000"
    val actorId = "200000000"
    println("Actor ID: $actorId")
    val token = RelationshipIDMain.encode(userId, permissions, actorId)
    println("Encoded: $token")
    println("Token length: ${token.length}")

    // Decode the token and print the decoded payload
    val decoded = RelationshipIDMain.decode(token)
    println("Decoded: $decoded")
}