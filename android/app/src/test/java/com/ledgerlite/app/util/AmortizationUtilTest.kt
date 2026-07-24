package com.ledgerlite.app.util

import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.domain.model.BigItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AmortizationUtilTest {

    private fun item(
        amount: Long,
        startDate: Long,
        status: BigItemStatus = BigItemStatus.active,
        endedAt: Long? = null
    ) = BigItem(
        name = "test",
        amount = amount,
        startDate = startDate,
        status = status,
        endedAt = endedAt,
        createdAt = 0,
        updatedAt = 0
    )

    private fun millis(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun `active使用中_天数从开始日到今天`() {
        // 开始日 = 今天，已使用 1 天
        val today = DateUtil.startOfToday()
        val it = item(100000, today)
        assertEquals(1, AmortizationUtil.totalDays(it, now = today))
    }

    @Test
    fun `active_开始3天前_已使用4天`() {
        val today = DateUtil.startOfToday()
        val start = today - 3L * 86_400_000L
        val it = item(100000, start)
        // 今天 - 开始日 + 1 = 3 + 1 = 4
        assertEquals(4, AmortizationUtil.totalDays(it, now = today))
    }

    @Test
    fun `ended_天数从开始日到结束日`() {
        val start = millis(2024, 1, 1)
        val end = millis(2024, 1, 10)
        val it = item(100000, start, BigItemStatus.ended, endedAt = end)
        // 1月1日到1月10日 = 10 天
        assertEquals(10, AmortizationUtil.totalDays(it, now = millis(2024, 6, 1)))
    }

    @Test
    fun `日均向下取整`() {
        val today = DateUtil.startOfToday()
        // 100 分 / 3 天 = 33.33 → 33
        val start = today - 2L * 86_400_000L // 共 3 天
        val it = item(100, start)
        assertEquals(33, AmortizationUtil.dailyCost(it, now = today))
        assertEquals(33 * 7, AmortizationUtil.weeklyCost(it, now = today))
    }

    @Test
    fun `ended状态不计入汇总`() {
        val today = DateUtil.startOfToday()
        val start = today - 9L * 86_400_000L // 共 10 天
        val active = item(30000, start, BigItemStatus.active)
        val ended = item(30000, start, BigItemStatus.ended, endedAt = today)
        assertTrue(AmortizationUtil.isActive(active))
        assertFalse(AmortizationUtil.isActive(ended))
        // active 日均 30000/10=3000，ended 不计入
        assertEquals(3000, AmortizationUtil.totalDailyCost(listOf(active, ended), now = today))
        assertEquals(21000, AmortizationUtil.totalWeeklyCost(listOf(active, ended), now = today))
    }

    @Test
    fun `天数最少为1`() {
        // 开始日在未来也不返回 0 或负数
        val future = DateUtil.startOfToday() + 10L * 86_400_000L
        val it = item(100000, future)
        assertEquals(1, AmortizationUtil.totalDays(it))
    }
}
