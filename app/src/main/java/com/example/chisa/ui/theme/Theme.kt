package com.example.chisa.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// ── 벚꽃 테마 ColorScheme ─────────────────────────────────────────────────────
// 설정에서 "벚꽃 테마" 토글 시 적용된다.
// dynamicColor / 다크모드와 무관하게 항상 벚꽃 팔레트를 사용한다.
private val SakuraColorScheme = lightColorScheme(
    primary      = SakuraPink,
    secondary    = SakuraPurple,
    background   = SakuraSurface,
    surface      = SakuraSurface,
    onPrimary    = Color.White,
    onSecondary  = Color.White,
    onBackground = SakuraBrown,
    onSurface    = SakuraBrown,
)

// ── CHISATheme ────────────────────────────────────────────────────────────────
// isSakura = true 이면 벚꽃 ColorScheme 을 사용한다.
// isSakura = false 이면 기존 동적 색상 / 다크모드 로직을 그대로 따른다.
//
// Parameters:
//   isSakura     : 벚꽃 테마 활성화 여부 (ViewModel 에서 전달)
//   darkTheme    : 시스템 다크모드 여부
//   dynamicColor : Android 12+ 동적 색상 사용 여부
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun CHISATheme(
    isSakura    : Boolean = false,
    darkTheme   : Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content     : @Composable () -> Unit
) {
    val colorScheme = when {
        isSakura -> SakuraColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}