package com.salespro.settings

import com.salespro.core.R
import java.text.NumberFormat
import java.util.Locale

/**
 * A currency the shop can display prices in.
 *
 * Amounts are stored in the database as hundredths (minor units = value * 100),
 * so [decimals] only controls how many decimal places are shown.
 */
data class CurrencyOption(
    val code: String,
    val symbol: String,
    val symbolBefore: Boolean,
    val decimals: Int,
    val nameRes: Int
)

object Currencies {
    val all: List<CurrencyOption> = listOf(
        CurrencyOption("EUR", "€", true, 2, R.string.cur_EUR),
        CurrencyOption("USD", "\$", true, 2, R.string.cur_USD),
        CurrencyOption("GBP", "£", true, 2, R.string.cur_GBP),
        CurrencyOption("TRY", "₺", true, 2, R.string.cur_TRY),
        CurrencyOption("SAR", "ر.س", false, 2, R.string.cur_SAR),
        CurrencyOption("AED", "د.إ", false, 2, R.string.cur_AED),
        CurrencyOption("KWD", "د.ك", false, 2, R.string.cur_KWD),
        CurrencyOption("QAR", "ر.ق", false, 2, R.string.cur_QAR),
        CurrencyOption("BHD", "د.ب", false, 2, R.string.cur_BHD),
        CurrencyOption("OMR", "ر.ع", false, 2, R.string.cur_OMR),
        CurrencyOption("JOD", "د.أ", false, 2, R.string.cur_JOD),
        CurrencyOption("IQD", "د.ع", false, 0, R.string.cur_IQD),
        CurrencyOption("SYP", "ل.س", false, 0, R.string.cur_SYP),
        CurrencyOption("LBP", "ل.ل", false, 0, R.string.cur_LBP),
        CurrencyOption("EGP", "ج.م", false, 2, R.string.cur_EGP),
        CurrencyOption("LYD", "ل.د", false, 2, R.string.cur_LYD),
        CurrencyOption("TND", "د.ت", false, 2, R.string.cur_TND),
        CurrencyOption("DZD", "د.ج", false, 2, R.string.cur_DZD),
        CurrencyOption("MAD", "د.م", false, 2, R.string.cur_MAD),
        CurrencyOption("SDG", "ج.س", false, 2, R.string.cur_SDG),
        CurrencyOption("YER", "ر.ي", false, 0, R.string.cur_YER),
        CurrencyOption("INR", "₹", true, 2, R.string.cur_INR)
    )

    fun find(code: String?): CurrencyOption =
        all.firstOrNull { it.code == code } ?: all.first { it.code == AppSettings.DEFAULT_CURRENCY }
}

/** Formats money using the currency selected in settings. */
object Money {
    /** [minor] is the stored value (hundredths). */
    fun format(minor: Long): String = formatMajor(minor / 100.0)

    /** [amount] is a normal value such as 12.5. Set [maxDecimals] to cap decimals (e.g. 0 for dashboards). */
    fun formatMajor(amount: Double, maxDecimals: Int? = null): String {
        val currency = AppSettings.currency
        val decimals = if (maxDecimals != null) minOf(maxDecimals, currency.decimals) else currency.decimals
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
            isGroupingUsed = true
        }
        val number = formatter.format(kotlin.math.abs(amount))
        val sign = if (amount < 0) "-" else ""
        return if (currency.symbolBefore) "$sign${currency.symbol}$number" else "$sign$number ${currency.symbol}"
    }

    fun symbol(): String = AppSettings.currency.symbol
}
