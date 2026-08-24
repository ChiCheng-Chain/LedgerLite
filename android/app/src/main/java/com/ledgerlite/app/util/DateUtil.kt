package com.ledgerlite.app.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import java.util.TimeZone

/**
 * 日历窗口计算。所有时间戳为 epoch millis。窗口计算集中在此，UI 层负责格式化。
 * 用设备默认时区。
 */
object DateUtil {

    fun nowMillis(): Long = System.currentTimeMillis()

    /** 本地时区相对 UTC 的毫秒偏移（如 UTC+8 = 28800000）。 */
    fun tzOffsetMillis(): Long = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()

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

    /**
     * 当前日 0 点的 Flow：订阅时立即发射，之后每跨一个本地 0 点重发。
     * 用于驱动「今日/本周/本月」等相对窗口在跨天后重新查询。
     * 发射的是触发器，不承载精确值——收到信号后应重新调用 startOfToday() 等函数。
     * [now] 供测试注入虚拟时钟，生产使用默认真实时钟。
     */
    fun observeDayStart(now: () -> Long = ::nowMillis): Flow<Long> = flow {
        while (true) {
            val current = now()
            emit(startOfDay(current))
            // 到下一个 0 点的时长，加 1 秒余量防止取整误差导致提前/反复触发
            val next = startOfNextDay(current) + 1_000L
            delay(next - current)
        }
    }
}
