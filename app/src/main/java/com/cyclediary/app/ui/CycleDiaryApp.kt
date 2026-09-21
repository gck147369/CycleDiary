package com.cyclediary.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyclediary.app.domain.CycleMath
import java.time.LocalDate

@Composable
fun CycleDiaryApp(viewModel: HomeViewModel = viewModel()) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // 「今天」只在回到前台时重新取一次：没有定时器、没有常驻任务，
    // 而手机上的 App 几乎每次看都是重新回到前台的，所以不会显示成昨天。
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        today = LocalDate.now()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            showSettings -> SettingsScreen(
                settings = data.settings,
                onBack = { showSettings = false },
                onSettingsChange = viewModel::updateSettings,
                onClearAll = viewModel::clearAll
            )

            else -> {
                val summary = CycleMath.summarize(data, today)
                HomeScreen(
                    data = data,
                    summary = summary,
                    today = today,
                    message = message,
                    onToggle = { viewModel.togglePeriod(today) },
                    onDeleteRecord = viewModel::deleteRecord,
                    onUpdateRecord = viewModel::updateRecord,
                    onOpenSettings = { showSettings = true },
                    onUndo = viewModel::undo,
                    onMessageShown = viewModel::consumeMessage
                )
            }
        }
    }
}
