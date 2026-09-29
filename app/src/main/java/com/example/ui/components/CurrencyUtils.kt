package com.example.ui.components

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val indonesianLocale = Locale("id", "ID")
    private val formatter = NumberFormat.getCurrencyInstance(indonesianLocale).apply {
        maximumFractionDigits = 0
    }

    fun formatRupiah(amount: Long): String {
        return try {
            formatter.format(amount).replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp %,d".format(amount).replace(',', '.')
        }
    }

    fun formatNumber(amount: Long): String {
        return "%,d".format(amount).replace(',', '.')
    }
}
