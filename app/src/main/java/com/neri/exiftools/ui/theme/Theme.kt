package com.neri.exiftools.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = OnriBow,
    onPrimary = OnriOnBow,
    primaryContainer = OnriBowContainer,
    onPrimaryContainer = OnriBowDeep,
    secondary = OnriCape,
    onSecondary = OnriOnCape,
    secondaryContainer = OnriCapeSoft,
    onSecondaryContainer = OnriCape,
    tertiary = OnriLilac,
    onTertiary = OnriInk,
    tertiaryContainer = OnriHair,
    onTertiaryContainer = OnriInk,
    background = OnriBg,
    onBackground = OnriInk,
    surface = OnriSurface,
    onSurface = OnriInk,
    surfaceVariant = OnriBgSoft,
    onSurfaceVariant = OnriInkSoft,
    outline = OnriOutline,
    error = OnriBowDeep,
    onError = OnriOnBow,
)

private val ColorOnDark = OnriDarkOn

private val DarkColors = darkColorScheme(
    primary = OnriDarkBow,
    onPrimary = OnriDarkBg,
    primaryContainer = OnriBowDeep,
    onPrimaryContainer = OnriBowContainer,
    secondary = OnriDarkCape,
    onSecondary = OnriDarkBg,
    secondaryContainer = OnriCape,
    onSecondaryContainer = OnriCapeSoft,
    tertiary = OnriDarkLilac,
    onTertiary = OnriDarkBg,
    background = OnriDarkBg,
    onBackground = ColorOnDark,
    surface = OnriDarkSurface,
    onSurface = ColorOnDark,
    surfaceVariant = OnriDarkField,
    onSurfaceVariant = OnriDarkMuted,
    outline = OnriDarkOutline,
    error = OnriDarkBow,
    onError = OnriDarkBg,
)

private val OnriShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun NeriExifTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = OnriShapes,
        content = content,
    )
}
