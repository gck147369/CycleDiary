package com.cyclediary.app.domain

import com.cyclediary.app.data.AppData
import com.cyclediary.app.data.endDate
import com.cyclediary.app.data.startDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class CyclePhase {
    /** 一条记录都还没有 */
    NO_DATA,

    /** 正在经期中（最后一条没写结束日期，或今天还在结束日期当天） */
    ON_PERIOD,

    /** 经期之间的日子 */
    NORMAL,

    /** 已经超过预测开始日，但还没记录新的经期 */
    LATE
}

/** 生理周期四个阶段。ON_PERIOD / LATE 这类"记录状态"不算阶段，所以单独一个枚举。 */
enum class CycleStage {
    PERIOD, FOLLICULAR, OVULATION, LUTEAL
}

/** 一个阶段覆盖周期第 fromDay 到第 toDay 天（闭区间），环形图按这些区间分段上色。 */
data class StageSpan(val stage: CycleStage, val fromDay: Int, val toDay: Int)

data class CycleSummary(
    val phase: CyclePhase = CyclePhase.NO_DATA,
    /** 今天处于哪个阶段；还没有任何记录时为 null */
    val stage: CycleStage? = null,
    /** 各阶段的天序区间，按时间顺序排列，拼起来正好覆盖 1..averageCycleLength；无记录时为空 */
    val stageSpans: List<StageSpan> = emptyList(),
    /** 估算的排卵日是周期第几天（下次经期开始日前 14 天）；无记录时为 null */
    val ovulationDay: Int? = null,
    /** 当前处于周期第几天（从最近一次经期第一天算起，从 1 开始） */
    val cycleDay: Int? = null,
    /** 若正在经期，这是经期第几天 */
    val periodDay: Int? = null,
    val lastStart: LocalDate? = null,
    val nextStart: LocalDate? = null,
    val daysUntilNext: Int = 0,
    val daysLate: Int = 0,
    val averageCycleLength: Int = 28,
    val averagePeriodLength: Int = 5,
    /** false 表示平均周期还是默认值，不是根据她自己的记录算的 */
    val basedOnOwnHistory: Boolean = false,
    val cycleProgress: Float = 0f,
    val recordCount: Int = 0
)

/**
 * 预测逻辑刻意做得保守和可解释：
 * 只用最近 6 次「相邻两次开始日期」的间隔求平均，并且丢掉明显不合理的值
 * （小于 15 天或大于 60 天），避免一次误触把预测带偏。
 */
object CycleMath {

    private const val MIN_PLAUSIBLE_CYCLE = 15
    private const val MAX_PLAUSIBLE_CYCLE = 60
    private const val MAX_PLAUSIBLE_PERIOD = 15
    private const val SAMPLE_SIZE = 6

    fun summarize(data: AppData, today: LocalDate): CycleSummary {
        val settings = data.settings

        val records = data.records
            .map { Triple(it, it.startDate(), it.endDate()) }
            .sortedBy { it.second }

        if (records.isEmpty()) {
            return CycleSummary(
                phase = CyclePhase.NO_DATA,
                averageCycleLength = settings.defaultCycleLength,
                averagePeriodLength = settings.defaultPeriodLength,
                recordCount = 0
            )
        }

        // ---- 平均周期长度 ----
        val gaps = records
            .map { it.second }
            .zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            .filter { it in MIN_PLAUSIBLE_CYCLE..MAX_PLAUSIBLE_CYCLE }
            .takeLast(SAMPLE_SIZE)

        val basedOnOwnHistory = gaps.isNotEmpty()
        val cycleLength =
            if (basedOnOwnHistory) gaps.average().roundToInt() else settings.defaultCycleLength

        // ---- 平均经期长度 ----
        val durations = records
            .mapNotNull { (_, start, end) -> end?.let { ChronoUnit.DAYS.between(start, it).toInt() + 1 } }
            .filter { it in 1..MAX_PLAUSIBLE_PERIOD }
            .takeLast(SAMPLE_SIZE)

        val periodLength =
            if (durations.isEmpty()) settings.defaultPeriodLength else durations.average().roundToInt()

        // ---- 当前状态 ----
        val last = records.last()
        val lastStart = last.second
        val lastEnd = last.third

        // 只有"还没写结束日期"才算正在经期中。
        // 如果她今天点了结束，今天就是这次经期的最后一天，此后进入新周期第 1 天——
        // 否则界面会在她说"结束了"之后仍然显示"经期中"，按钮也翻不回"今天来了"。
        val onPeriod = lastEnd == null
        val rawCycleDay = ChronoUnit.DAYS.between(lastStart, today).toInt() + 1
        val cycleDay = rawCycleDay.coerceAtLeast(1)

        val nextStart = lastStart.plusDays(cycleLength.toLong())
        val daysUntilNext = ChronoUnit.DAYS.between(today, nextStart).toInt()

        val phase = when {
            onPeriod -> CyclePhase.ON_PERIOD
            daysUntilNext < 0 -> CyclePhase.LATE
            else -> CyclePhase.NORMAL
        }

        // ---- 生理阶段 ----
        // 标准估算：排卵日 = 下次经期开始日前 14 天；易孕窗口 = 排卵日前 5 天到当天。
        // 阶段区间可能互相挤压（比如周期只有 18 天时经期会顶到易孕窗口），
        // 所以每一层的起点都要向后一层看齐，保证区间不重叠、不越界。
        val safePeriodLength = periodLength.coerceIn(1, cycleLength)
        // 排卵日至少排在经期结束后，避免超短周期时阶段互相重叠
        val ovulationDay = (cycleLength - 14).coerceAtLeast(safePeriodLength + 1)
        val fertileStart = (ovulationDay - 5).coerceAtLeast(safePeriodLength + 1)

        val spans = buildList {
            add(StageSpan(CycleStage.PERIOD, 1, safePeriodLength))
            if (fertileStart > safePeriodLength + 1) {
                add(StageSpan(CycleStage.FOLLICULAR, safePeriodLength + 1, fertileStart - 1))
            }
            add(StageSpan(CycleStage.OVULATION, fertileStart, ovulationDay))
            if (cycleLength > ovulationDay) {
                add(StageSpan(CycleStage.LUTEAL, ovulationDay + 1, cycleLength))
            }
        }

        // 已推迟说明黄体期被拉长了，按黄体期显示比显示"超出范围"更符合直觉
        val stage = when {
            onPeriod -> CycleStage.PERIOD
            cycleDay > ovulationDay -> CycleStage.LUTEAL
            cycleDay >= fertileStart -> CycleStage.OVULATION
            cycleDay > safePeriodLength -> CycleStage.FOLLICULAR
            else -> CycleStage.PERIOD
        }

        return CycleSummary(
            phase = phase,
            stage = stage,
            stageSpans = spans,
            ovulationDay = ovulationDay,
            cycleDay = cycleDay,
            periodDay = if (onPeriod) cycleDay else null,
            lastStart = lastStart,
            nextStart = nextStart,
            daysUntilNext = daysUntilNext,
            daysLate = if (daysUntilNext < 0) -daysUntilNext else 0,
            averageCycleLength = cycleLength,
            averagePeriodLength = periodLength,
            basedOnOwnHistory = basedOnOwnHistory,
            cycleProgress = (cycleDay.toFloat() / cycleLength.toFloat()).coerceIn(0f, 1f),
            recordCount = records.size
        )
    }
}
