package com.ledgerlite.app.util

/**
 * 金额转换：分（Long）↔ 元字符串。所有金额内部用 Long 分，UI 展示用元字符串。
 * 1234 分 → "12.34"；0 分 → "0.00"；负数支持。
 */
object MoneyUtil {

    /**
     * 分 → 元字符串。decimalPlaces 控制小数位数：
     * - 2：固定两位（1234 → "12.34"）
     * - 1：固定一位，截断（1234 → "12.3"）
     * - 0：四舍五入到元（1250 → "13"），不输出小数段
     * withGrouping 控制千分位逗号。
     */
    fun centsToYuan(cents: Long, withGrouping: Boolean = true, decimalPlaces: Int = 2): String {
        val negative = cents < 0
        val abs = if (negative) -cents else cents
        val places = decimalPlaces.coerceIn(0, 2)
        val yuanStr: String
        val fracStr: String
        when (places) {
            0 -> {
                val yuan = (abs + 50) / 100
                yuanStr = if (withGrouping) formatGrouping(yuan) else yuan.toString()
                fracStr = ""
            }
            1 -> {
                val jiao = abs / 10                  // 1234 分 → 123 角
                val yuan = jiao / 10                 // 12 元
                val frac = jiao % 10                 // 3 角
                yuanStr = if (withGrouping) formatGrouping(yuan) else yuan.toString()
                fracStr = ".$frac"
            }
            else -> {
                val yuan = abs / 100
                val frac = abs % 100
                yuanStr = if (withGrouping) formatGrouping(yuan) else yuan.toString()
                fracStr = ".${frac.toString().padStart(2, '0')}"
            }
        }
        val result = yuanStr + fracStr
        return if (negative) "-$result" else result
    }

    /** 元字符串（如 "12.34"）→ 分。解析失败返回 0。支持千分位逗号。 */
    fun yuanToCents(yuan: String): Long {
        val trimmed = yuan.trim().replace(",", "")
        if (trimmed.isEmpty()) return 0
        val negative = trimmed.startsWith("-")
        val positive = trimmed.removePrefix("-")
        val parts = positive.split(".")
        val yuanPart = parts[0].toLongOrNull() ?: 0L
        val fracPart = if (parts.size > 1) {
            val frac = parts[1]
            when {
                frac.isEmpty() -> 0L
                frac.length == 1 -> (frac + "0").toLongOrNull() ?: 0L
                frac.length == 2 -> frac.toLongOrNull() ?: 0L
                else -> frac.substring(0, 2).toLongOrNull() ?: 0L
            }
        } else 0L
        val cents = yuanPart * 100 + fracPart
        return if (negative) -cents else cents
    }

    /** 数字键盘输入串（如 "1234" 表示 12.34）→ 分。用于记账面板输入。 */
    fun inputToCents(input: String): Long {
        if (input.isEmpty()) return 0
        val digits = input.filter { it.isDigit() }
        if (digits.isEmpty()) return 0
        return digits.toLong()
    }

    /** 分 → 展示串（数字键盘视角）：1234 分 → "12.34"。等同 centsToYuan 不带千分位。 */
    fun centsToInput(cents: Long): String = centsToYuan(cents, withGrouping = false)

    private fun formatGrouping(value: Long): String {
        val s = value.toString()
        return s.reversed().chunked(3).joinToString(",").reversed()
    }
}
