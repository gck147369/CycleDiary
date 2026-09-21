package com.cyclediary.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 全部数据 = App 私有目录里的一个 JSON 文件。
 *
 * 为什么不用数据库：这个 App 一辈子就存几十条记录，
 * 一个文件足够，而且少一层依赖就少一种周末卡住的可能。
 * 这个目录其他 App 读不到，App 也没有联网权限，数据出不去。
 */
class CycleStore(context: Context) {

    private val appContext = context.applicationContext

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val mutex = Mutex()

    private val file: File get() = File(appContext.filesDir, DATA_FILE)

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()

    /**
     * 启动时同步读一次。
     *
     * 这个文件满打满算也就几十条记录、几 KB，读一次是零点几毫秒，
     * 换来的是「没有加载态、没有首屏闪烁、数据一定先于界面就位」。
     * 写入仍然走 IO 线程，不在主线程上做写操作。
     */
    init {
        val loaded = runCatching {
            if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
        }.getOrElse { AppData() }
        _data.value = loaded.normalized()
    }

    suspend fun update(transform: (AppData) -> AppData) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val next = transform(_data.value).normalized()
            writeAtomically(next)
            _data.value = next
        }
    }

    /**
     * 先写临时文件再改名，避免写入过程中被杀进程导致文件半截。
     */
    private fun writeAtomically(value: AppData) {
        val text = json.encodeToString(AppData.serializer(), value)
        val tmp = File(file.parentFile, "$DATA_FILE.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    private companion object {
        const val DATA_FILE = "cycle-diary.json"
    }
}
