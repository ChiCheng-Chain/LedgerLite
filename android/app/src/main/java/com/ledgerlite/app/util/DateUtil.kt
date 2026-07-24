package com.ledgerlite.app.util

import java.util.Calendar
import java.util.TimeZone

/**
 * 日历窗口计算。所有时间戳为 epoch millis。窗口计算集中在此，UI 层负责格式化。
 * 用设备默认时区。
 */
object DateUtil {

    fun nowMillis(): Long = System.currentTimeMillis()

    /** 今日 0 点 epoch millis（本地时区）。 */
    fun startOfToday(): Long = startOfDay(nowMillis())

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** 本月 1 日 0 点。 */
    fun startOfMonth(millis: Long = nowMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** 下月 1 日 0 点（本月结束边界，开区间）。 */
    fun startOfNextMonth(millis: Long = nowMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** 明日 0 点（今日结束边界，开区间）。 */
    fun startOfNextDay(millis: Long = nowMillis()): Long = startOfDay(millis) + 86_400_000L

    /** 本周一 0 点（周一为一周起点）。 */
    fun startOfWeek(millis: Long = nowMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // 周一为一周第一天：FIRST_DAY_OF_WEEK=MONDAY 在 Locale.CHINA 默认即如此，
        // 但 Calendar.get(DAY_OF_WEEK) 周日=1，需减到周一
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val offset = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -offset)
        return cal.timeInMillis
    }

    /** N 天前 0 点。 */
    fun startDaysAgo(days: Int, from: Long = nowMillis()): Long =
        startOfDay(from) - days * 86_400_000L
}
