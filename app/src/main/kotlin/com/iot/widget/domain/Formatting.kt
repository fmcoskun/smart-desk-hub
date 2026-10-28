package com.iot.widget.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

/**
 * Domain layer · display formatting. Never uses the phone's locale for numbers: a Turkish
 * phone would otherwise show "22,4°C" (the reference fixed the same bug with Locale.ROOT).
 */
object Formatting {
    /** One decimal, rounded half-up like JavaScript's toFixed(1): 22.44 → "22.4°C". */
    fun temperature(celsius: Double): String =
        BigDecimal(celsius).setScale(1, RoundingMode.HALF_UP).toPlainString() + "°C"

    fun humidity(percent: Double): String = "${percent.roundToInt()}%"

    fun watts(watts: Int): String = "${watts}W"

    fun percent(value: Int): String = "$value%"
}
