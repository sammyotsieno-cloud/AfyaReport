package ke.co.afyareport.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightScheme = lightColorScheme(
    primary = Indigo, onPrimary = Color.White,
    primaryContainer = IndigoSoft, onPrimaryContainer = IndigoDeep,
    secondary = Stamp, onSecondary = Color.White,
    secondaryContainer = StampSoft, onSecondaryContainer = StampDeep,
    tertiary = Amber, onTertiary = Color.White,
    tertiaryContainer = AmberSoft, onTertiaryContainer = AmberDeep,
    background = Paper, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = PaperSunk, onSurfaceVariant = InkSoft,
    surfaceContainerLowest = PaperRaised, surfaceContainerLow = Paper,
    surfaceContainer = PaperMid, surfaceContainerHigh = PaperSunk,
    surfaceContainerHighest = PaperDeep,
    outline = RuleStrong, outlineVariant = Rule,
    error = Alert, onError = Color.White,
    errorContainer = AlertSoft, onErrorContainer = Alert
)

private val DarkScheme = darkColorScheme(
    primary = NightIndigo, onPrimary = IndigoDeep,
    primaryContainer = NightIndigoSoft, onPrimaryContainer = IndigoSoft,
    secondary = NightStamp, onSecondary = StampDeep,
    secondaryContainer = NightStampSoft, onSecondaryContainer = StampSoft,
    tertiary = NightAmber, onTertiary = AmberDeep,
    tertiaryContainer = NightAmberSoft, onTertiaryContainer = AmberSoft,
    background = NightPaper, onBackground = NightInk,
    surface = NightPaper, onSurface = NightInk,
    surfaceVariant = NightDeep, onSurfaceVariant = NightInkSoft,
    surfaceContainerLowest = NightSunk, surfaceContainerLow = NightMid,
    surfaceContainer = NightRaised, surfaceContainerHigh = NightDeep,
    surfaceContainerHighest = NightDeep,
    outline = NightRuleStrong, outlineVariant = NightRule,
    error = NightAlert, onError = Alert,
    errorContainer = NightAlertSoft, onErrorContainer = AlertSoft
)

private val AfyaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AfyaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AfyaTypography,
        shapes = AfyaShapes,
        content = content
    )
}
