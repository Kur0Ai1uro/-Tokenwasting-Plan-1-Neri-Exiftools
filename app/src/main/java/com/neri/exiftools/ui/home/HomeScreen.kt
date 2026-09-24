package com.neri.exiftools.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neri.exiftools.ui.theme.OnriAvatar
import com.neri.exiftools.ui.theme.OnriBowMark
import com.neri.exiftools.ui.theme.OnriGlassCard
import com.neri.exiftools.ui.theme.OnriPomDivider
import com.neri.exiftools.ui.theme.OnriStage
import com.neri.exiftools.ui.theme.OnriXClip
import com.neri.exiftools.ui.SaveLocationSection
import com.neri.exiftools.util.SaveLocation

@Composable
fun HomeScreen(
    isLoading: Boolean,
    saveLocation: SaveLocation,
    onPickImage: () -> Unit,
    onApplySavePath: (String) -> Unit,
    onPickSaveFolder: () -> Unit,
    onResetSaveLocation: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    OnriStage(mascotAlpha = 0.48f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OnriAvatar(size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "音理 ExifTools",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onBackground,
                    )
                    Text(
                        text = "风又音理的照片记录本",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.primary,
                    )
                }
                OnriBowMark(modifier = Modifier.size(22.dp), color = scheme.primary)
            }

            Spacer(Modifier.weight(1f))

            OnriGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OnriAvatar(size = 124.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "把照片交给音理吧",
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OnriXClip(modifier = Modifier.size(12.dp), color = scheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "治愈系元数据小助手",
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.primary,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    OnriPomDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "JPEG、PNG、WebP 都可以看。保存前音理会先把原图备份好。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onPickImage,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                    disabledContainerColor = scheme.primary.copy(alpha = 0.4f),
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = scheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    OnriBowMark(modifier = Modifier.size(18.dp), color = scheme.onPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (isLoading) "音理正在翻看…" else "选择图片")
            }

            Spacer(Modifier.height(18.dp))
            SaveLocationSection(
                location = saveLocation,
                enabled = !isLoading,
                onApplyPath = onApplySavePath,
                onPickFolder = onPickSaveFolder,
                onReset = onResetSaveLocation,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "v1.2",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
