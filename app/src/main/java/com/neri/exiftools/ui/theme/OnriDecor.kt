package com.neri.exiftools.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neri.exiftools.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalOnriMascot = staticCompositionLocalOf<ImageBitmap?> { null }

@Composable
fun rememberOnriMascotBitmap(): ImageBitmap? = rememberMascotBitmap(enabled = true)

@Composable
fun OnriStage(
    modifier: Modifier = Modifier,
    mascotAlpha: Float = 0.16f,
    showDecor: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = !isLightBackground()
    val scheme = MaterialTheme.colorScheme
    val sharedMascot = LocalOnriMascot.current
    val mascot = sharedMascot ?: rememberMascotBitmap(enabled = mascotAlpha > 0f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        if (mascotAlpha > 0f && mascot != null) {
            Image(
                bitmap = mascot,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                alpha = mascotAlpha,
                colorFilter = if (dark) {
                    ColorFilter.tint(OnriDarkBg.copy(alpha = 0.22f), BlendMode.Multiply)
                } else {
                    null
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to scheme.background.copy(alpha = 0.06f),
                            0.38f to scheme.background.copy(alpha = 0.04f),
                            0.72f to scheme.background.copy(alpha = 0.24f),
                            1f to scheme.background.copy(alpha = 0.58f),
                        ),
                    ),
            )
        }
        if (showDecor) {
            OnriFlower(
                color = if (dark) OnriDarkLilac.copy(alpha = 0.32f) else OnriPetalBlue.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 18.dp, top = 24.dp)
                    .size(34.dp),
            )
            OnriFlower(
                color = if (dark) OnriDarkBow.copy(alpha = 0.26f) else OnriPetalPink.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 22.dp, top = 56.dp)
                    .size(26.dp)
                    .rotate(18f),
            )
            OnriFlower(
                color = if (dark) OnriDarkCape.copy(alpha = 0.22f) else OnriHair.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 28.dp, bottom = 92.dp)
                    .size(22.dp)
                    .rotate(-12f),
            )
            OnriHeart(
                color = if (dark) OnriDarkBow.copy(alpha = 0.28f) else OnriHeart.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 26.dp, bottom = 120.dp)
                    .size(18.dp)
                    .rotate(16f),
            )
            OnriSparkles(dark)
        }
        content()
    }
}

@Composable
fun onriTextFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        disabledTextColor = scheme.onSurface.copy(alpha = 0.5f),
        focusedContainerColor = scheme.surface,
        unfocusedContainerColor = scheme.surface,
        disabledContainerColor = scheme.surface.copy(alpha = 0.7f),
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = scheme.outline,
        focusedLabelColor = scheme.primary,
        unfocusedLabelColor = scheme.onSurface,
        disabledLabelColor = scheme.onSurface.copy(alpha = 0.5f),
        focusedPlaceholderColor = scheme.onSurface.copy(alpha = 0.55f),
        unfocusedPlaceholderColor = scheme.onSurface.copy(alpha = 0.55f),
        cursorColor = scheme.primary,
        focusedTrailingIconColor = scheme.onSurface,
        unfocusedTrailingIconColor = scheme.onSurface,
    )
}

@Composable
private fun rememberMascotBitmap(enabled: Boolean): ImageBitmap? {
    val context = LocalContext.current
    return remember(enabled) {
        if (!enabled) return@remember null
        val resources = context.resources
        val target = maxOf(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, R.drawable.onri_mascot, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@remember null
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= target) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeResource(resources, R.drawable.onri_mascot, options)?.asImageBitmap()
    }
}

@Composable
private fun isLightBackground(): Boolean {
    return MaterialTheme.colorScheme.background == OnriBg
}

@Composable
private fun BoxScope.OnriSparkles(dark: Boolean) {
    val color = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.9f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dots = listOf(
            0.18f to 0.22f,
            0.82f to 0.18f,
            0.12f to 0.63f,
            0.88f to 0.58f,
            0.73f to 0.34f,
            0.28f to 0.78f,
        )
        dots.forEach { (x, y) ->
            drawCircle(
                color = color,
                radius = 3.2f * density,
                center = Offset(size.width * x, size.height * y),
            )
        }
    }
}

@Composable
fun OnriAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    ring: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val ringWidth = if (ring) 3.dp else 0.dp
    Box(
        modifier = modifier
            .size(size + ringWidth * 2)
            .clip(CircleShape)
            .background(if (ring) scheme.primary else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.onri_portrait),
            contentDescription = "风又音理",
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(scheme.primaryContainer),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun OnriGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surface.copy(alpha = 0.82f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
fun OnriFlower(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val petal = radius * 0.46f
        val distance = radius * 0.38f
        for (index in 0 until 4) {
            val angle = index * (PI / 2.0)
            drawCircle(
                color = color,
                radius = petal,
                center = Offset(
                    center.x + cos(angle).toFloat() * distance,
                    center.y + sin(angle).toFloat() * distance,
                ),
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.72f),
            radius = radius * 0.22f,
            center = center,
        )
    }
}

@Composable
fun OnriHeart(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            moveTo(w / 2f, h * 0.82f)
            cubicTo(w * 0.1f, h * 0.52f, w * 0.02f, h * 0.18f, w / 2f, h * 0.32f)
            cubicTo(w * 0.98f, h * 0.18f, w * 0.9f, h * 0.52f, w / 2f, h * 0.82f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun OnriXClip(modifier: Modifier = Modifier, color: Color = OnriInk, stroke: Dp = 3.dp) {
    Canvas(modifier = modifier) {
        val pad = size.minDimension * 0.18f
        val strokeWidth = stroke.toPx()
        drawLine(color, Offset(pad, pad), Offset(size.width - pad, size.height - pad), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(size.width - pad, pad), Offset(pad, size.height - pad), strokeWidth, StrokeCap.Round)
    }
}

@Composable
fun OnriBowMark(modifier: Modifier = Modifier, color: Color = OnriBow) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val left = Path().apply {
            moveTo(w * 0.46f, h * 0.5f)
            cubicTo(w * 0.08f, h * 0.05f, w * 0.02f, h * 0.95f, w * 0.46f, h * 0.5f)
            close()
        }
        val right = Path().apply {
            moveTo(w * 0.54f, h * 0.5f)
            cubicTo(w * 0.92f, h * 0.05f, w * 0.98f, h * 0.95f, w * 0.54f, h * 0.5f)
            close()
        }
        drawPath(left, color)
        drawPath(right, color)
        drawCircle(color, radius = size.minDimension * 0.16f, center = Offset(w / 2f, h / 2f))
        drawCircle(Color.White.copy(alpha = 0.45f), radius = size.minDimension * 0.07f, center = Offset(w / 2f, h / 2f))
    }
}

@Composable
fun OnriSectionTitle(text: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnriBowMark(modifier = Modifier.size(18.dp), color = scheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = scheme.outline.copy(alpha = 0.8f),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(scheme.secondary, CircleShape),
        )
    }
}

@Composable
fun OnriPomDivider() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(scheme.secondary, CircleShape))
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            color = scheme.outline,
        )
        OnriBowMark(Modifier.size(16.dp), color = scheme.primary)
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            color = scheme.outline,
        )
        Box(Modifier.size(8.dp).background(scheme.secondary, CircleShape))
    }
}
