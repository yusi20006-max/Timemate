package ir.timemate.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Stable semantic tokens retained for non-theme-specific utility colors.
val DeepBlack = Color(0xFF07090E)
val MidnightBlue = Color(0xFF0F172A)
val RoyalAzure = Color(0xFF2563EB)
val DeepAzure = Color(0xFF1E3A8A)
val NeonViolet = Color(0xFF8B5CF6)
val SoftPurple = Color(0xFFA78BFA)
val IceWhite = Color(0xFFF8FAFC)
val CoolSilver = Color(0xFF94A3B8)
val GlassSurface = Color(0x14FFFFFF)
val GlassBorder = Color(0x28FFFFFF)
val UrgentRed = Color(0xFFEF4444)
val EmeraldSuccess = Color(0xFF10B981)

private data class ThemePalette(
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color
)

private fun palette(theme: AppTheme) = when (theme) {
    AppTheme.DEEP_OCEAN -> ThemePalette(
        Color(0xFF397BFF), Color(0xFF1E3A8A), Color(0xFF9B65FF), Color(0xFFB8A0FF),
        Color(0xFF07090E), Color(0xFF10182B), Color(0xFF18243A), Color(0xFFF5F7FF),
        Color(0xFFA8B4CC), Color(0xFF344766), Color(0xFFFF647C)
    )
    AppTheme.MIDNIGHT_ROSE -> ThemePalette(
        Color(0xFFE54869), Color(0xFF641D35), Color(0xFFFF65A8), Color(0xFFFFA0C4),
        Color(0xFF0D080D), Color(0xFF21121C), Color(0xFF321927), Color(0xFFFFF3F7),
        Color(0xFFD0AAB9), Color(0xFF694052), Color(0xFFFF6B75)
    )
    AppTheme.FOREST_NIGHT -> ThemePalette(
        Color(0xFF16A875), Color(0xFF14533F), Color(0xFF20C9B0), Color(0xFF8AE6D1),
        Color(0xFF060D0B), Color(0xFF10221C), Color(0xFF18352A), Color(0xFFF0FFF8),
        Color(0xFFA4C9B8), Color(0xFF315D4C), Color(0xFFFF7777)
    )
}

@Composable
fun TimeMateTheme(theme: AppTheme = AppTheme.DEEP_OCEAN, content: @Composable () -> Unit) {
    val colors = palette(theme)
    val scheme = darkColorScheme(
        primary = colors.primary,
        onPrimary = Color.White,
        primaryContainer = colors.primaryContainer,
        onPrimaryContainer = Color.White,
        secondary = colors.secondary,
        onSecondary = Color.White,
        tertiary = colors.tertiary,
        onTertiary = Color(0xFF101010),
        background = colors.background,
        onBackground = colors.onSurface,
        surface = colors.surface,
        onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.onSurfaceVariant,
        outline = colors.outline,
        outlineVariant = colors.outline.copy(alpha = 0.55f),
        error = colors.error
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    borderColor: Color? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.onSurface.copy(alpha = 0.07f),
                        colors.primary.copy(alpha = 0.10f),
                        colors.secondary.copy(alpha = 0.08f)
                    )
                )
            )
            .border(1.dp, borderColor ?: colors.outlineVariant, shape)
            .padding(contentPadding),
        content = content
    )
}
