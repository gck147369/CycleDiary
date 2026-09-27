package com.cyclediary.app

import android.os.Looper
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 冒烟测试：真的把 MainActivity 启起来，用真实的界面元素点一遍。
 * 覆盖「启动不崩 → 记录开始 → 记录结束 → 打开历史 → 进设置」这条主链路。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        // 注意：Robolectric 下不能直接用 composeRule.waitUntil —— 它只推进虚拟时钟，
        // 不会真正驱动主 looper，组合永远不发生。这里手动推 looper 并重试。
        repeat(50) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
            val found = composeRule.onAllNodesWithText(text, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
            if (found) return
            Thread.sleep(20)
        }
        throw AssertionError("界面上一直没出现这段文字：$text")
    }

    @Test
    fun fullHappyPath() {
        // 1. 冷启动：应该停在空状态，并且不崩
        waitForText("还没有记录")
        composeRule.onNodeWithText("今天来了").performClick()

        // 2. 记录开始后：按钮换成「今天结束了」，历史里出现一条进行中
        waitForText("今天结束了")
        waitForText("进行中")

        // 3. 记录结束：按钮换回「今天来了」，这条记录变成已结束
        composeRule.onNodeWithText("今天结束了").performClick()
        waitForText("今天来了")
        waitForText("持续 1 天")

        // 4. 点开这条记录能弹出编辑对话框，再关掉
        composeRule.onNodeWithText("持续 1 天").performClick()
        waitForText("修改这次记录")
        composeRule.onNodeWithText("取消").performClick()
        waitForText("历史记录")

        // 5. 进设置再返回
        composeRule.onNodeWithContentDescription("设置").performClick()
        waitForText("周期长度")
        composeRule.onNodeWithContentDescription("返回").performClick()
        waitForText("历史记录")
    }
}
