/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Material 3 in Compose
 * Type: [Source Code]
 * Available at: <https://developer.android.com/develop/ui/compose/designsystems/material3> [Accessed 9 September 2026]
 **************************************************************************************************************/

package za.co.yebopets.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YeboPetsColors = lightColorScheme(
    primary = YeboCoral,
    onPrimary = YeboInk,
    primaryContainer = YeboCoralSoft,
    onPrimaryContainer = YeboInk,
    secondary = YeboTeal,
    onSecondary = YeboInk,
    secondaryContainer = YeboTealSoft,
    onSecondaryContainer = YeboInk,
    tertiary = YeboSun,
    onTertiary = YeboInk,
    tertiaryContainer = YeboSunSoft,
    onTertiaryContainer = YeboInk,
    background = YeboCream,
    onBackground = YeboInk,
    surface = YeboPaper,
    onSurface = YeboInk,
    surfaceVariant = YeboSage,
    onSurfaceVariant = YeboMuted,
    outline = YeboOutline,
    outlineVariant = YeboOutline.copy(alpha = 0.72f),
    error = YeboError,
    onError = Color.White,
)

@Composable
fun YeboPetsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YeboPetsColors,
        typography = Typography,
        content = content,
    )
}
