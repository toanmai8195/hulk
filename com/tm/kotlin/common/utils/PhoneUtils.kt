package com.tm.kotlin.common.utils

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
}