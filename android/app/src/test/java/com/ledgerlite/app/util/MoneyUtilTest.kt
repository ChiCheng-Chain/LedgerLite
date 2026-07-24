package com.ledgerlite.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyUtilTest {

    @Test
    fun `分转元_基本`() {
        assertEquals("0.00", MoneyUtil.centsToYuan(0))
        assertEquals("12.34", MoneyUtil.centsToYuan(1234))
        assertEquals("1.00", MoneyUtil.centsToYuan(100))
        assertEquals("0.05", MoneyUtil.centsToYuan(5))
    }

    @Test
    fun `分转元_千分位`() {
        assertEquals("1,234.56", MoneyUtil.centsToYuan(123456))
        assertEquals("1,000,000.00", MoneyUtil.centsToYuan(100000000))
    }

    @Test
    fun `分转元_负数`() {
        assertEquals("-12.34", MoneyUtil.centsToYuan(-1234))
    }

    @Test
    fun `元转分_基本`() {
        assertEquals(1234, MoneyUtil.yuanToCents("12.34"))
        assertEquals(0, MoneyUtil.yuanToCents("0"))
        assertEquals(100, MoneyUtil.yuanToCents("1"))
        assertEquals(5, MoneyUtil.yuanToCents("0.05"))
    }

    @Test
    fun `元转分_小数位不足补零`() {
        assertEquals(1200, MoneyUtil.yuanToCents("12.0"))
        assertEquals(1230, MoneyUtil.yuanToCents("12.3"))
        assertEquals(1234, MoneyUtil.yuanToCents("12.345")) // 截断到两位
    }

    @Test
    fun `元转分_空串与非法`() {
        assertEquals(0, MoneyUtil.yuanToCents(""))
        assertEquals(0, MoneyUtil.yuanToCents("abc"))
    }

    @Test
    fun `数字键盘输入转分`() {
        assertEquals(0, MoneyUtil.inputToCents(""))
        assertEquals(12, MoneyUtil.inputToCents("12"))
        assertEquals(1234, MoneyUtil.inputToCents("1234"))
    }

    @Test
    fun `往返一致`() {
        assertEquals(123456, MoneyUtil.yuanToCents(MoneyUtil.centsToYuan(123456)))
    }
}
