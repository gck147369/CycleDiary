package com.cyclediary.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.cyclediary.app.data.AppData
import com.cyclediary.app.data.PeriodRecord
import com.cyclediary.app.data.Settings
import com.cyclediary.app.domain.CycleMath
import com.cyclediary.app.ui.HomeScreen
import com.cyclediary.app.ui.SettingsScreen
import com.cyclediary.app.ui.theme.CycleDiaryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * 把界面渲染成 PNG，用来肉眼检查「好不好看」和有没有布局事故。
 * 走 Robolectric 原生图形模式在 JVM 上真实光栅化，不需要模拟器、不需要真机。
 *
 *   gradlew.bat testDebugUnitTest
 *
 * 产物在 app/build/screenshots/。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class UiScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 9, 21)

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            CycleDiaryTheme(darkTheme = dark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    content()
                }
            }
        }
        composeRule.waitForIdle()

        val view = composeRule.activity.window.decorView
        val metrics = composeRule.activity.resources.displayMetrics
        if (view.width == 0 || view.height == 0) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        }

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val dir = File("build/screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun captureHome(name: String, data: AppData, dark: Boolean = false) =
        capture(name, dark) {
            HomeScreen(
                data = data,
                summary = CycleMath.summarize(data, today),
                today = today,
                message = null,
                onToggle = {},
                onDeleteRecord = {},
                onUpdateRecord = { _, _, _ -> },
                onOpenSettings = {},
                onUndo = {},
                onMessageShown = {}
            )
        }

    /** 三次已结束的经期，周期长度约 28 天 */
    private val history = listOf(
        PeriodRecord(id = "1", start = "2026-07-01", end = "2026-07-05"),
        PeriodRecord(id = "2", start = "2026-07-29", end = "2026-08-02"),
        PeriodRecord(id = "3", start = "2026-08-26", end = "2026-08-30")
    )

    @Test
    fun homeOnPeriod() {
        // 9 月 19 日开始、还没结束 -> 9 月 21 日应是「经期中 / 第 3 天」
        captureHome(
            name = "01-home-on-period",
            data = AppData(records = history + PeriodRecord(id = "4", start = "2026-09-19"))
        )
    }

    @Test
    fun homeMidCycle() {
        // 最后一次 8 月 26 日开始 -> 9 月 21 日是周期第 27 天，距下次 2 天
        captureHome(name = "02-home-mid-cycle", data = AppData(records = history))
    }

    @Test
    fun homeEmpty() {
        captureHome(name = "03-home-empty", data = AppData())
    }

    @Test
    fun homeDark() {
        captureHome(
            name = "04-home-dark",
            data = AppData(records = history + PeriodRecord(id = "4", start = "2026-09-19")),
            dark = true
        )
    }

    @Test
    fun settings() {
        capture("05-settings") {
            SettingsScreen(
                settings = Settings(),
                onBack = {},
                onSettingsChange = {},
                onClearAll = {}
            )
        }
    }
}
