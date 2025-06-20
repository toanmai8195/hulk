package com.tm.kotlin.common.utils

import com.tm.kotlin.common.utils.PhoneUtils.containsUnlinkedUrl
import java.security.MessageDigest

object PhoneUtils {
    fun generateVietnamPhoneNumber(): String {
        val prefixes = listOf(
            "032", "033", "034", "035", "036", "037", "038", "039", // Viettel
            "070", "076", "077", "078", "079",                     // MobiFone
            "081", "082", "083", "084", "085",                     // VinaPhone
            "056", "058",                                           // Vietnamobile
            "059"                                                  // Gmobile
        )

        val prefix = prefixes.random()
//        val number = (1000000..1005000).random() // 7 số còn lại
        val number = (1000000..9999999).random() // 7 số còn lại

        return prefix + number.toString()
    }

    fun containsUnlinkedUrl(text: String): Boolean {
        // Regex phát hiện các URL phổ biến
        val urlRegex = Regex("""\b(?:https?://)?(?:www\.)?(tinyurl\.com|bit\.ly|t\.co|goo\.gl|is\.gd|buff\.ly|rebrand\.ly|lnkd\.in|[a-z0-9\-]+\.[a-z]{2,})/\S+""", RegexOption.IGNORE_CASE)

        // Regex phát hiện <a href="...">...</a>
        val htmlLinkRegex = Regex("""<a\s+[^>]*href=["'][^"']+["'][^>]*>.*?</a>""", RegexOption.IGNORE_CASE)

        // Regex phát hiện markdown link [text](url)
        val markdownLinkRegex = Regex("""\[[^\]]+]\((https?://[^\s)]+)\)""", RegexOption.IGNORE_CASE)

        val rawUrls = urlRegex.findAll(text).map { it.value }.toList()
        val linkedText = htmlLinkRegex.findAll(text).joinToString(" ") + markdownLinkRegex.findAll(text).joinToString(" ")

        for (url in rawUrls) {
            if (!linkedText.contains(url)) {
                return true // Có URL chưa được chèn link
            }
        }

        return false
    }
}

fun hashRequestId(requestId: String, oaId: String, userId: String): String {
    val input = "${oaId}_${userId}_${requestId.ifEmpty { System.currentTimeMillis().toString() }}"
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }.substring(0, 32)
}

fun main() {
   println(hashRequestId("1a732286-d1ff-4db1-892f-2dd62c63daae","9933291","0981878486"))
}