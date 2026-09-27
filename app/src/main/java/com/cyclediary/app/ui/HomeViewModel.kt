package com.cyclediary.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclediary.app.data.AppData
import com.cyclediary.app.data.CycleStore
import com.cyclediary.app.data.PeriodRecord
import com.cyclediary.app.data.Settings
import com.cyclediary.app.data.endDate
import com.cyclediary.app.data.startDate
import com.cyclediary.app.domain.CycleMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class UiMessage(val text: String, val undoable: Boolean)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val store = CycleStore(application.applicationContext)

    val data: StateFlow<AppData> = store.data

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    /** 只保存"最近一次改动之前"的完整数据，用于短时间撤回误触。 */
    private var undoSnapshot: AppData? = null

    /**
     * 记录变动后，把自动算出的平均周期/经期同步进设置，
     * 设置页里看到的就始终是当前真实值，而不是某次手动填的旧数。
     * 记录不足两次时 CycleMath 会返回 null，设置保持原样。
     */
    private fun syncSettings(current: AppData): AppData =
        CycleMath.syncedSettings(current)?.let { current.copy(settings = it) } ?: current

    /**
     * 一个按钮管两件事：没有未结束的记录时 = 记录"今天开始"，
     * 否则 = 把那条未结束的记录补上"今天结束"。
     * 这样就不可能重复开始，也不会出现两条同时进行中。
     */
    fun togglePeriod(today: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val before = store.data.value
            var text = ""
            store.update { current ->
                // 取最后一条未结束的：万一数据被改出多条未结束，也该收尾最近那条
                val open = current.records.lastOrNull { it.end.isNullOrBlank() }
                if (open == null) {
                    text = "已记录：今天经期开始"
                    syncSettings(
                        current.copy(
                            records = current.records + PeriodRecord(
                                id = UUID.randomUUID().toString(),
                                start = today.toString(),
                                end = null
                            )
                        )
                    )
                } else {
                    val start = open.startDate()
                    val end = if (today.isBefore(start)) start else today
                    text = if (end == start) "已记录：这次经期只有一天" else "已记录：这次经期结束"
                    syncSettings(
                        current.copy(
                            records = current.records.map { r ->
                                if (r.id == open.id) r.copy(end = end.toString()) else r
                            }
                        )
                    )
                }
            }
            undoSnapshot = before
            _message.value = UiMessage(text, undoable = true)
        }
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            val before = store.data.value
            store.update { current -> syncSettings(current.copy(records = current.records.filterNot { it.id == id })) }
            undoSnapshot = before
            _message.value = UiMessage("已删除这条记录", undoable = true)
        }
    }

    fun updateRecord(id: String, start: LocalDate, end: LocalDate?) {
        viewModelScope.launch {
            val before = store.data.value
            val safeEnd = end?.takeIf { !it.isBefore(start) }
            store.update { current ->
                syncSettings(
                    current.copy(
                        records = current.records.map { r ->
                            if (r.id == id) {
                                r.copy(start = start.toString(), end = safeEnd?.toString())
                            } else {
                                r
                            }
                        }
                    )
                )
            }
            undoSnapshot = before
            _message.value = UiMessage("已更新记录", undoable = true)
        }
    }

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            store.update { it.copy(settings = settings) }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            undoSnapshot = null
            store.update { it.copy(records = emptyList()) }
            _message.value = UiMessage("已清空全部记录", undoable = false)
        }
    }

    fun undo() {
        val snapshot = undoSnapshot ?: return
        undoSnapshot = null
        viewModelScope.launch {
            store.update { current -> syncSettings(snapshot) }
            _message.value = UiMessage("已撤回", undoable = false)
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
