package com.cyclediary.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyclediary.app.data.PeriodRecord
import com.cyclediary.app.data.endDate
import com.cyclediary.app.data.startDate
import java.time.LocalDate

private enum class PickerField { START, END }

/**
 * 误触是必然会发生的（忘了点"结束"、点错日期），所以记录必须能改能删。
 * 修改后预测会自动重算，因为预测是每次从记录现场算出来的，不存中间结果。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordDialog(
    record: PeriodRecord,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate?) -> Unit,
    onDelete: () -> Unit
) {
    var start by remember(record.id) { mutableStateOf(record.startDate()) }
    var end by remember(record.id) { mutableStateOf(record.endDate()) }
    var picker by remember(record.id) { mutableStateOf<PickerField?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改这次记录") },
        text = {
            Column {
                DateRow(
                    label = "开始日期",
                    value = start.formatMonthDayWeek(),
                    onClick = { picker = PickerField.START }
                )
                Spacer(Modifier.height(8.dp))
                DateRow(
                    label = "结束日期",
                    value = end?.formatMonthDayWeek() ?: "未结束",
                    onClick = { picker = PickerField.END }
                )
                if (end != null) {
                    TextButton(
                        onClick = { end = null },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("清空结束日期（表示还在经期中）")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(start, end?.takeIf { !it.isBefore(start) }) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )

    val target = picker
    if (target != null) {
        val initial = if (target == PickerField.START) start else (end ?: start)
        val todayMillis = today.toUtcMillis()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial.toUtcMillis(),
            selectableDates = object : SelectableDates {
                // 不允许选未来的日期：经期不可能"提前记录"
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { picker = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val picked = millis.toLocalDateUtc()
                            if (target == PickerField.START) start = picked else end = picked
                        }
                        picker = null
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { picker = null }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

@Composable
private fun DateRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
