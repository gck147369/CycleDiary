package com.cyclediary.app.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

/**
 * 一次经期记录。日期一律用 ISO 字符串（yyyy-MM-dd）存储，
 * 这样 JSON 文件用记事本打开也能直接看懂。
 *
 * end == null 表示"经期还没结束"。
 */
@Serializable
data class PeriodRecord(
    val id: String,
    val start: String,
    val end: String? = null
)

@Serializable
data class Settings(
    /** 记录不足 2 次、算不出真实周期时，用它来预测 */
    val defaultCycleLength: Int = 28,
    /** 还没有一次完整记录时，用它来估算经期长度 */
    val defaultPeriodLength: Int = 5
)

@Serializable
data class AppData(
    val records: List<PeriodRecord> = emptyList(),
    val settings: Settings = Settings()
)

fun PeriodRecord.startDate(): LocalDate = LocalDate.parse(start)

fun PeriodRecord.endDate(): LocalDate? = end?.let { LocalDate.parse(it) }

/**
 * 清洗数据：丢掉解析不了的脏数据、修正"结束早于开始"、按开始日期排序。
 * 每次读写都会过一遍，保证界面上永远不会出现莫名其妙的值。
 */
fun AppData.normalized(): AppData {
    val cleaned = ArrayList<PeriodRecord>(records.size)
    for (r in records) {
        val start = runCatching { LocalDate.parse(r.start) }.getOrNull() ?: continue
        val end = r.end?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        cleaned += PeriodRecord(
            id = r.id.ifBlank { UUID.randomUUID().toString() },
            start = start.toString(),
            end = end?.takeIf { !it.isBefore(start) }?.toString()
        )
    }
    return copy(records = cleaned.distinctBy { it.id }.sortedBy { it.start })
}
