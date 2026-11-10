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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider

// Deep Black, Royal Azure Blue, and Neon Violet Palette
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

private val TimeMateColorScheme = darkColorScheme(
    primary = RoyalAzure,
    secondary = NeonViolet,
    tertiary = SoftPurple,
    background = DeepBlack,
    surface = MidnightBlue,
    onPrimary = IceWhite,
    onSecondary = IceWhite,
    onBackground = IceWhite,
    onSurface = IceWhite
)

@Composable
fun TimeMateTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = TimeMateColorScheme,
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    borderColor: Color = GlassBorder,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.09f),
                        RoyalAzure.copy(alpha = 0.06f),
                        NeonViolet.copy(alpha = 0.05f)
                    )
                )
            )
            .border(1.dp, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}
