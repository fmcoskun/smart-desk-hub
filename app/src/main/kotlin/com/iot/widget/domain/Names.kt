package com.iot.widget.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Domain layer · comparing names the way a person would: ignoring case and surrounding spaces,
 * and treating the Turkish i's as one letter. Used for users and devices alike.
 */
object Names {
    /**
     * Turkish has two i's: "YILMAZ" lowercases to "yilmaz" but the stored "Yılmaz" to "yılmaz"
     * (dotless ı), and "İ" lowercases to "i" plus a combining dot. Folding ı/i/İ together makes
     * "AYŞE YILMAZ" match "Ayşe Yılmaz".
     */
    fun comparable(name: String): String =
        clean(name).lowercase(Locale.ROOT).replace("\u0307", "").replace('\u0131', 'i')

    fun sameName(a: String, b: String): Boolean = comparable(a) == comparable(b)

    /**
     * Trimmed and in Unicode NFC form. "ş" can arrive as one character or as "s" plus a
     * combining cedilla (some keyboards, pasted text); NFC turns both into the same one.
     */
    fun clean(name: String): String = Normalizer.normalize(name.trim(), Normalizer.Form.NFC)

    /**
     * Length in code points, so a simple emoji counts as one, not two UTF-16 units. (Combined emoji
     * such as a flag or a skin tone are still several code points; that is fine for a length limit.)
     */
    fun length(name: String): Int = name.codePointCount(0, name.length)
}
