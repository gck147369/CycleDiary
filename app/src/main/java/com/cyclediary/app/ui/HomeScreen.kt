package com.cyclediary.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyclediary.app.data.AppData
import com.cyclediary.app.data.PeriodRecord
import com.cyclediary.app.data.endDate
import com.cyclediary.app.data.startDate
import com.cyclediary.app.domain.CyclePhase
import com.cyclediary.app.domain.CycleStage
import com.cyclediary.app.domain.CycleSummary
import com.cyclediary.app.ui.theme.colorIn
import com.cyclediary.app.ui.theme.DarkStageColors
import com.cyclediary.app.ui.theme.LightStageColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun HomeScreen(
    data: AppData,
    summary: CycleSummary,
    today: LocalDate,
    message: UiMessage?,
    onToggle: () -> Unit,
    onDeleteRecord: (String) -> Unit,
    onUpdateRecord: (String, LocalDate, LocalDate?) -> Unit,
    onOpenSettings: () -> Unit,
    onUndo: () -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var editingRecord by remember { mutableStateOf<PeriodRecord?>(null) }

    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = current.text,
            actionLabel = if (current.undoable) "撤回" else null,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) onUndo()
        onMessageShown()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            HeaderRow(onOpenSettings)
            Spacer(Modifier.height(18.dp))
            HeroCard(summary = summary, today = today)
            if (summary.phase != CyclePhase.NO_DATA) {
                // 一条记录都没有的时候，这四格全是"—"，只是噪音，直接不显示
                Spacer(Modifier.height(14.dp))
                StatsCard(summary)
            }
            Spacer(Modifier.height(22.dp))
            PrimaryActionButton(summary = summary, onClick = onToggle)
            Spacer(Modifier.height(30.dp))
            HistorySection(records = data.records, onEdit = { editingRecord = it })
            Spacer(Modifier.height(36.dp))
        }
    }

    editingRecord?.let { record ->
        EditRecordDialog(
            record = record,
            today = today,
            onDismiss = { editingRecord = null },
            onSave = { start, end ->
                onUpdateRecord(record.id, start, end)
                editingRecord = null
            },
            onDelete = {
                onDeleteRecord(record.id)
                editingRecord = null
            }
        )
    }
}

@Composable
private fun HeaderRow(onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "周期日记",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeroCard(summary: CycleSummary, today: LocalDate) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = today.formatMonthDayWeek(),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                if (summary.phase == CyclePhase.ON_PERIOD) {
                    PhasePill("经期中")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (summary.phase == CyclePhase.NO_DATA) {
                Text("还没有记录", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "点下面的按钮，记下经期的第一天。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            } else {
                Spacer(Modifier.height(6.dp))

                CycleRing(summary = summary, modifier = Modifier.align(Alignment.CenterHorizontally))

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stageStatusText(summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun PhasePill(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

private fun stageStatusText(summary: CycleSummary): String {
    val stage = summary.stage ?: return ""
    // 大字已经在说「第 N 天」了，这里只说她此刻还想知道的事：处于哪个阶段、接下来怎样
    val stageName = when (stage) {
        CycleStage.PERIOD -> "经期中"
        CycleStage.FOLLICULAR -> "卵泡期"
        CycleStage.OVULATION -> "排卵期"
        CycleStage.LUTEAL -> "黄体期"
    }
    val detail = when (summary.phase) {
        CyclePhase.ON_PERIOD -> {
            val day = summary.periodDay ?: 1
            val remaining = summary.averagePeriodLength - day
            when {
                remaining > 0 -> "预计还有 $remaining 天结束"
                remaining == 0 -> "预计今天就结束"
                else -> "比平均经期长了 ${-remaining} 天"
            }
        }
        CyclePhase.LATE -> "预计开始日已经过了 ${summary.daysLate} 天"
        else -> when (summary.daysUntilNext) {
            0 -> "预计今天开始"
            1 -> "距下次经期还有 1 天"
            else -> "距下次经期还有 ${summary.daysUntilNext} 天"
        }
    }
    return "$stageName · $detail"
}

/**
 * 环形周期图：整圈代表一个完整周期，按阶段分段上色，
 * 环上的小圆点标出「今天」的位置，中间的大字还是周期天数。
 */
@Composable
private fun CycleRing(summary: CycleSummary, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val stageColors = if (isSystemInDarkTheme()) DarkStageColors else LightStageColors
    val strokeWidth = 16.dp
    val day = summary.cycleDay ?: 1
    val cycleLength = summary.averageCycleLength

    Box(modifier = modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = min(size.width, size.height) - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val strokeStyle = Stroke(width = strokePx, cap = StrokeCap.Butt)

            // 底圈：压暗一档，让四段颜色浮在上面
            drawArc(
                color = scheme.onPrimaryContainer.copy(alpha = 0.12f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = strokeStyle
            )

            // 每段之间留一点缝，四个阶段才读得清；单段时（极端短周期）不做缝
            val gapDegrees = if (summary.stageSpans.size > 1) 3f else 0f
            var angle = -90f
            for (span in summary.stageSpans) {
                val sweep = (span.toDay - span.fromDay + 1) / cycleLength.toFloat() * 360f
                drawArc(
                    color = span.stage.colorIn(stageColors),
                    startAngle = angle + gapDegrees / 2f,
                    sweepAngle = (sweep - gapDegrees).coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = topLeft, size = arcSize, style = strokeStyle
                )
                angle += sweep
            }

            // 「今天」标记：小圆环，压在当前这一天中间
            val markerAngle = Math.toRadians(((day - 0.5f) / cycleLength * 360f - 90f).toDouble())
            val radius = diameter / 2f
            val center = Offset(
                topLeft.x + diameter / 2f + radius * cos(markerAngle).toFloat(),
                topLeft.y + diameter / 2f + radius * sin(markerAngle).toFloat()
            )
            // 外圈用卡片底色抠出白边，避免和阶段色糊在一起
            drawCircle(scheme.primaryContainer, radius = 7.dp.toPx(), center = center)
            drawCircle(scheme.onPrimaryContainer, radius = 4.dp.toPx(), center = center)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "第",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$day",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "天",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}

@Composable
private fun StatsCard(summary: CycleSummary) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = scheme.onSurface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row {
                StatBlock(
                    label = "下次经期",
                    value = summary.nextStart?.formatMonthDay() ?: "—",
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = "距今",
                    value = distanceText(summary),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(Modifier.height(16.dp))

            Row {
                StatBlock(
                    label = "平均周期",
                    value = "${summary.averageCycleLength} 天",
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = "平均经期",
                    value = "${summary.averagePeriodLength} 天",
                    modifier = Modifier.weight(1f)
                )
            }

            if (summary.phase != CyclePhase.NO_DATA && !summary.basedOnOwnHistory) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "平均周期目前用默认值估算，记录满两次后就会按你自己的周期来算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun distanceText(summary: CycleSummary): String = when {
    summary.phase == CyclePhase.NO_DATA -> "—"
    summary.daysLate > 0 -> "推迟 ${summary.daysLate} 天"
    summary.daysUntilNext == 0 -> "就是今天"
    else -> "${summary.daysUntilNext} 天"
}

@Composable
private fun PrimaryActionButton(summary: CycleSummary, onClick: () -> Unit) {
    val ending = summary.phase == CyclePhase.ON_PERIOD
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = if (ending) {
            // 用主色容器色，和上面的卡片是同一套色，深色模式下也不会灰成一片
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            ButtonDefaults.buttonColors()
        }
    ) {
        Icon(
            imageVector = if (ending) Icons.Filled.Check else Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (ending) "今天结束了" else "今天来了",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun HistorySection(
    records: List<PeriodRecord>,
    onEdit: (PeriodRecord) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "历史记录",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (records.isEmpty()) "" else "共 ${records.size} 次",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(10.dp))

    if (records.isEmpty()) {
        Text(
            text = "这里会列出每一次经期，点一下就能改日期或删除。",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = scheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column {
                records.sortedByDescending { it.start }.forEachIndexed { index, record ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = scheme.outlineVariant.copy(alpha = 0.45f),
                            modifier = Modifier.padding(start = 20.dp)
                        )
                    }
                    HistoryRow(record = record, onEdit = onEdit)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(record: PeriodRecord, onEdit: (PeriodRecord) -> Unit) {
    val start = record.startDate()
    val end = record.endDate()

    Surface(
        onClick = { onEdit(record) },
        color = Color.Transparent,
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (end == null) {
                        "${start.formatMonthDay()} 起"
                    } else {
                        "${start.formatMonthDay()} – ${end.formatMonthDay()}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (end == null) {
                        "进行中"
                    } else {
                        "持续 ${ChronoUnit.DAYS.between(start, end).toInt() + 1} 天"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
