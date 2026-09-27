package com.cyclediary.app.domain

import com.cyclediary.app.data.AppData
import com.cyclediary.app.data.PeriodRecord
import com.cyclediary.app.data.Settings
import com.cyclediary.app.data.normalized
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 预测是这个 App 唯一"算"出来的东西，所以规则要有测试兜着。
 * 顺便当作预测规则的可执行文档。
 */
class PredictionTest {

    private val today = LocalDate.of(2026, 9, 21)

    private fun record(id: String, start: String, end: String? = null) =
        PeriodRecord(id = id, start = start, end = end)

    @Test
    fun emptyDataShowsNoData() {
        val s = CycleMath.summarize(AppData(), today)
        assertEquals(CyclePhase.NO_DATA, s.phase)
        assertNull(s.cycleDay)
        assertEquals(28, s.averageCycleLength)
        assertFalse(s.basedOnOwnHistory)
    }

    @Test
    fun periodStartingTodayIsDayOne() {
        val s = CycleMath.summarize(
            AppData(records = listOf(record("1", "2026-09-21"))),
            today
        )
        assertEquals(CyclePhase.ON_PERIOD, s.phase)
        assertEquals(1, s.cycleDay)
        assertEquals(1, s.periodDay)
        // 没有历史时用默认 28 天
        assertEquals(LocalDate.of(2026, 10, 19), s.nextStart)
    }

    @Test
    fun periodEndedTodayIsNotOngoing() {
        // 今天点了"结束"之后，就应该进入新周期第 1 天，
        // 而不是继续显示"经期中"
        val s = CycleMath.summarize(
            AppData(records = listOf(record("1", "2026-09-19", "2026-09-21"))),
            today
        )
        assertEquals(CyclePhase.NORMAL, s.phase)
        assertNull(s.periodDay)
        assertEquals(3, s.cycleDay)
    }

    @Test
    fun regularHistoryGivesRealAverage() {
        val s = CycleMath.summarize(
            AppData(
                records = listOf(
                    record("1", "2026-07-01", "2026-07-05"),
                    record("2", "2026-07-29", "2026-08-02"),
                    record("3", "2026-08-26", "2026-08-30")
                )
            ),
            today
        )
        assertEquals(CyclePhase.NORMAL, s.phase)
        assertTrue(s.basedOnOwnHistory)
        assertEquals(28, s.averageCycleLength)
        assertEquals(5, s.averagePeriodLength)
        // 8 月 26 日到 9 月 21 日是周期第 27 天
        assertEquals(27, s.cycleDay)
        assertEquals(LocalDate.of(2026, 9, 23), s.nextStart)
        assertEquals(2, s.daysUntilNext)
    }

    @Test
    fun implausibleGapIsIgnored() {
        // 中间夹了一次误触（相隔只有 5 天），不能把平均值带偏
        val s = CycleMath.summarize(
            AppData(
                records = listOf(
                    record("1", "2026-07-01", "2026-07-05"),
                    record("2", "2026-07-06", "2026-07-10"),
                    record("3", "2026-08-03", "2026-08-07")
                )
            ),
            today
        )
        assertTrue(s.basedOnOwnHistory)
        assertEquals(28, s.averageCycleLength)
    }

    @Test
    fun overduePeriodIsReportedAsLate() {
        val s = CycleMath.summarize(
            AppData(
                records = listOf(
                    record("1", "2026-07-04", "2026-07-08"),
                    record("2", "2026-08-01", "2026-08-05")
                )
            ),
            today
        )
        assertEquals(CyclePhase.LATE, s.phase)
        assertEquals(LocalDate.of(2026, 8, 29), s.nextStart)
        assertEquals(23, s.daysLate)
        assertTrue(s.daysUntilNext < 0)
    }

    @Test
    fun ongoingPeriodIsNotCountedInAverageLength() {
        val s = CycleMath.summarize(
            AppData(
                records = listOf(
                    record("1", "2026-07-01", "2026-07-04"),   // 4 天
                    record("2", "2026-07-29", "2026-08-03"),   // 6 天
                    record("3", "2026-08-26")                  // 还没结束，不参与平均
                )
            ),
            today
        )
        assertEquals(5, s.averagePeriodLength)
    }

    @Test
    fun normalizeDropsAndFixesBadRecords() {
        val dirty = AppData(
            records = listOf(
                PeriodRecord("a", "2026-09-10", "2026-09-08"), // 结束早于开始 -> 结束被清掉
                PeriodRecord("b", "不是日期", "2026-09-08"),      // 解析失败 -> 整条丢弃
                PeriodRecord("c", "2026-09-01", "2026-09-05"),
                PeriodRecord("c", "2026-09-01", "2026-09-05")  // 重复 id -> 去重
            )
        )
        val clean = dirty.normalized()
        assertEquals(2, clean.records.size)
        assertEquals(listOf("c", "a"), clean.records.map { it.id }) // 按开始日期升序
        assertNull(clean.records.first { it.id == "a" }.end)
    }

    @Test
    fun recentGapsCountMoreThanOldOnes() {
        // 加权平均：最近一次间隔 34 天，前五次都是 28 天。
        // 简单平均是 29，加权后近期权重更大，结果是 30 —— 近期漂移被更快反映出来
        val starts = listOf("2026-04-06", "2026-05-04", "2026-06-01", "2026-06-29", "2026-07-27", "2026-08-30")
        val s = CycleMath.summarize(
            AppData(records = starts.mapIndexed { i, day ->
                record(i.toString(), day, LocalDate.parse(day).plusDays(4).toString())
            }),
            today
        )
        assertEquals(30, s.averageCycleLength)
    }

    @Test
    fun recentPeriodLengthsCountMore() {
        // 加权平均：最近一次经期 6 天，之前是 3,4,4,4,4 天。
        // 简单平均是 4（四舍五入），加权后被最近的一次拉到 5
        val durations = listOf(3, 4, 4, 4, 4, 6)
        var start = LocalDate.of(2026, 4, 6)
        val records = durations.mapIndexed { i, d ->
            val r = record(i.toString(), start.toString(), start.plusDays((d - 1).toLong()).toString())
            start = start.plusDays(28)
            r
        }
        val s = CycleMath.summarize(AppData(records = records), today)
        assertEquals(5, s.averagePeriodLength)
    }

    @Test
    fun syncedSettingsFollowAutoAverage() {
        // 记录足够时，设置里的值应该被同步成自动算出的平均值
        val data = AppData(
            records = listOf(
                record("1", "2026-07-04", "2026-07-08"),   // 经期 5 天
                record("2", "2026-08-01", "2026-08-04")    // 经期 4 天，间隔 28 天
            ),
            settings = Settings(defaultCycleLength = 30, defaultPeriodLength = 6)
        )
        val synced = CycleMath.syncedSettings(data)!!
        // 两次经期加权平均：(5*1 + 4*2) / 3 = 4.33 -> 4
        assertEquals(4, synced.defaultPeriodLength)
        assertEquals(28, synced.defaultCycleLength)

        // 已经同步过（值一致）时不再产生新设置
        val again = CycleMath.syncedSettings(data.copy(settings = synced))
        assertNull(again)
    }

    @Test
    fun syncedSettingsReturnsNullWithoutEnoughData() {
        assertNull(CycleMath.syncedSettings(AppData()))
        // 只有一条记录：算不出间隔，不动设置
        assertNull(
            CycleMath.syncedSettings(
                AppData(records = listOf(record("1", "2026-09-01", "2026-09-05")))
            )
        )
    }
}
