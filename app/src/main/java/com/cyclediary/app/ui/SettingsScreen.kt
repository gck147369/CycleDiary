package com.cyclediary.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cyclediary.app.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onBack: () -> Unit,
    onSettingsChange: (Settings) -> Unit,
    onClearAll: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    NumberRow(
                        title = "默认周期长度",
                        subtitle = "记录不足两次时，先用它来预测",
                        value = settings.defaultCycleLength,
                        range = 20..45,
                        onChange = { onSettingsChange(settings.copy(defaultCycleLength = it)) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    NumberRow(
                        title = "默认经期长度",
                        subtitle = "还没有完整记录时用它估算",
                        value = settings.defaultPeriodLength,
                        range = 2..10,
                        onChange = { onSettingsChange(settings.copy(defaultPeriodLength = it)) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "关于这个 App",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    InfoLine("数据只保存在这台手机上，卸载 App 即删除")
                    InfoLine("没有申请任何系统权限，也没有联网能力")
                    InfoLine("没有广告、没有账号、没有推送、没有后台服务")
                    InfoLine("预测只是根据你记录的平均值估算，仅供参考")
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = { confirmClear = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("清空全部记录")
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "版本 1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空全部记录？") },
            text = { Text("所有经期记录都会被删掉，无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun NumberRow(
    title: String,
    subtitle: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = { onChange(value - 1) },
            enabled = value > range.first
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 40.dp)
        )
        IconButton(
            onClick = { onChange(value + 1) },
            enabled = value < range.last
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Row(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(
            text = "·",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.widthIn(min = 8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
