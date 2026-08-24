package com.ledgerlite.app.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
class DateUtilTest {

    @Test
    fun `订阅时立即发射当前日0点`() = runTest {
        val emitted = DateUtil.observeDayStart().first()
        assertEquals(DateUtil.startOfToday(), emitted)
    }

    @Test
    fun `跨零点后重发新一天的0点`() = runTest {
        // 虚拟时钟：与 TestScheduler 时间同步推进
        val virtualNow = AtomicLong(System.currentTimeMillis())
        val observe = {
            DateUtil.observeDayStart { virtualNow.get() }
        }
        // 先看第一个延迟挂多久，把虚拟时钟推到临近 0 点，再推进剩余部分触发重发
        val first = observe().first()
        val toMidnight = DateUtil.startOfNextDay(virtualNow.get()) + 1_000L - virtualNow.get()

        val deferred = async { observe().take(2).toList() }
        runCurrent()
        // 每次推进后让虚拟时钟跟上调度器时间，模拟真实时间流逝
        advanceTimeBy(toMidnight)
        virtualNow.set(DateUtil.startOfNextDay(virtualNow.get()) + 2_000L)
        advanceTimeBy(60_000L)
        runCurrent()
        val emissions = deferred.await()
        assertEquals(2, emissions.size)
        assertEquals(first, emissions[0])
        assertEquals(DateUtil.startOfNextDay(first), emissions[1])
        assertTrue(emissions[1] > emissions[0])
    }
}
