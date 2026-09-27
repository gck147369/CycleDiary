package com.cyclediary.app.domain

import com.cyclediary.app.data.AppData
import com.cyclediary.app.data.PeriodRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 生理阶段（卵泡期 / 排卵期 / 黄体期）的估算规则文档。
 * 规则：排卵日 = 下次经期开始日前 14 天；易孕窗口 = 排卵日前 5 天到当天。
 */
class StageTest {

    private val today = LocalDate.of(2026, 9, 21)

    private fun record(id: String, start: String, end: String? = null) =
        PeriodRecord(id = id, start = start, end = end)

    private fun summarize(vararg records: PeriodRecord) =
        CycleMath.summarize(AppData(records = records.toList()), today)

    @Test
    fun standardCycleSpansCoverExactlyOneCycle() {
        // 平均周期 28 天、经期 5 天：排卵日是第 14 天，易孕窗口第 9~14 天
        val s = summarize(
            record("1", "2026-07-01", "2026-07-05"),
            record("2", "2026-07-29", "2026-08-02"),
            record("3", "2026-08-26", "2026-08-30")
        )
        assertEquals(CycleStage.LUTEAL, s.stage)   // 周期第 27 天
        assertEquals(14, s.ovulationDay)
        assertEquals(
            listOf(
                StageSpan(CycleStage.PERIOD, 1, 5),
                StageSpan(CycleStage.FOLLICULAR, 6, 8),
                StageSpan(CycleStage.OVULATION, 9, 14),
                StageSpan(CycleStage.LUTEAL, 15, 28)
            ),
            s.stageSpans
        )
    }

    @Test
    fun spansAreContiguousAndInBounds() {
        val s = summarize(
            record("1", "2026-07-04", "2026-07-08"),
            record("2", "2026-08-01", "2026-08-05"),
            record("3", "2026-08-29", "2026-09-02")
        )
        var expected = 1
        for (span in s.stageSpans) {
            assertEquals(expected, span.fromDay)
            assertTrue(span.toDay >= span.fromDay)
            assertTrue(span.toDay <= s.averageCycleLength)
            expected = span.toDay + 1
        }
        // 拼起来正好铺满一整个周期
        assertEquals(s.averageCycleLength + 1, expected)
    }

    @Test
    fun shortCycleDoesNotOverlapStages() {
        // 周期短到排卵日会被经期顶住：阶段区间互相挤压但不重叠
        val s = summarize(
            record("1", "2026-07-01", "2026-07-05"),
            record("2", "2026-07-19", "2026-07-23"),
            record("3", "2026-08-06", "2026-08-10"),
            record("4", "2026-08-24")    // 今天 9-21，处于推迟状态
        )
        val days = s.stageSpans.flatMap { it.fromDay..it.toDay }
        assertEquals(days.size, days.distinct().size)          // 不重叠
        assertEquals((1..s.averageCycleLength).toList(), days.sorted()) // 不越界、无空洞
    }

    @Test
    fun latePeriodStillShowsLutealStage() {
        // 推迟的日子里，黄体期只是被拉长了，不应该显示成"没有阶段"
        val s = summarize(
            record("1", "2026-07-04", "2026-07-08"),
            record("2", "2026-08-01", "2026-08-05")
        )
        assertEquals(CyclePhase.LATE, s.phase)
        assertEquals(CycleStage.LUTEAL, s.stage)
    }

    @Test
    fun ongoingPeriodShowsPeriodStage() {
        val s = summarize(record("1", "2026-09-19"))
        assertEquals(CyclePhase.ON_PERIOD, s.phase)
        assertEquals(CycleStage.PERIOD, s.stage)
        assertEquals(1, s.stageSpans.first().fromDay)
    }

    @Test
    fun noDataHasNoStage() {
        val s = CycleMath.summarize(AppData(), today)
        assertEquals(null, s.stage)
        assertEquals(emptyList<StageSpan>(), s.stageSpans)
    }

    @Test
    fun defaultCycleUsesDefaultLengths() {
        // 只有一次已结束的记录：按默认 28 天周期、5 天经期估算
        val s = summarize(record("1", "2026-09-01", "2026-09-05"))
        assertEquals(CyclePhase.NORMAL, s.phase)
        assertEquals(CycleStage.LUTEAL, s.stage)   // 周期第 21 天，已过排卵日
    }
}
