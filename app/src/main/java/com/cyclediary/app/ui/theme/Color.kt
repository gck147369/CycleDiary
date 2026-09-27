package com.cyclediary.app.ui.theme

import androidx.compose.material3.darkColorScheme
import com.cyclediary.app.domain.CycleStage
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 配色刻意选择「雾蓝 + 石板灰」的中性色。
 * 不用粉色系，也不跟随壁纸取色（dynamic color）——跟随壁纸就有可能变成粉色，
 * 那样就违背了"不要刻板印象风格"这条要求。
 */
val LightColors = lightColorScheme(
    primary = Color(0xFF2C6A8A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9E6F6),
    onPrimaryContainer = Color(0xFF001E2C),
    secondary = Color(0xFF4F616B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E5F1),
    onSecondaryContainer = Color(0xFF0A1E28),
    tertiary = Color(0xFF5B5B7D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0DFFF),
    onTertiaryContainer = Color(0xFF181937),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F9FB),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFF7F9FB),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDCE3E8),
    onSurfaceVariant = Color(0xFF41484D),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CD),
    inverseSurface = Color(0xFF2E3133),
    inverseOnSurface = Color(0xFFEFF1F3),
    inversePrimary = Color(0xFF93CCEA)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF93CCEA),
    onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF164B65),
    onPrimaryContainer = Color(0xFFC9E6F6),
    secondary = Color(0xFFB7C9D6),
    onSecondary = Color(0xFF21333D),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFD2E5F1),
    tertiary = Color(0xFFC4C3EA),
    onTertiary = Color(0xFF2D2E4C),
    tertiaryContainer = Color(0xFF434465),
    onTertiaryContainer = Color(0xFFE0DFFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101417),
    onBackground = Color(0xFFE1E2E6),
    surface = Color(0xFF101417),
    onSurface = Color(0xFFE1E2E6),
    surfaceVariant = Color(0xFF41484D),
    onSurfaceVariant = Color(0xFFC1C7CD),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF41484D),
    inverseSurface = Color(0xFFE1E2E6),
    inverseOnSurface = Color(0xFF2E3133),
    inversePrimary = Color(0xFF2C6A8A)
)

/**
 * 四个生理阶段的专用色。选色原则和主色一样：低饱和、柔和不刺眼，
 * 并且和雾蓝主色放在一起不打架（玫瑰红 / 青绿 / 暖橙 / 灰紫）。
 * 亮色和暗色各一套——暗色下直接用亮色版本会显得发闷或过艳。
 */
data class StageColors(
    val period: Color,
    val follicular: Color,
    val ovulation: Color,
    val luteal: Color
)

val LightStageColors = StageColors(
    period = Color(0xFFD9707A),
    follicular = Color(0xFF5BA58C),
    ovulation = Color(0xFFD99A4E),
    luteal = Color(0xFF8A7FC4)
)

val DarkStageColors = StageColors(
    period = Color(0xFFE68D95),
    follicular = Color(0xFF7FC3AA),
    ovulation = Color(0xFFE7B476),
    luteal = Color(0xFFA89FD6)
)

fun CycleStage.colorIn(colors: StageColors): Color = when (this) {
    CycleStage.PERIOD -> colors.period
    CycleStage.FOLLICULAR -> colors.follicular
    CycleStage.OVULATION -> colors.ovulation
    CycleStage.LUTEAL -> colors.luteal
}
