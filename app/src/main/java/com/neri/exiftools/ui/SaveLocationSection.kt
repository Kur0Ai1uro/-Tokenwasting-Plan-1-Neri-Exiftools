package com.neri.exiftools.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.neri.exiftools.ui.theme.onriTextFieldColors
import com.neri.exiftools.util.SaveLocation

@Composable
fun SaveLocationSection(
    location: SaveLocation,
    enabled: Boolean,
    onApplyPath: (String) -> Unit,
    onPickFolder: () -> Unit,
    onReset: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = onriTextFieldColors()
    val focusManager = LocalFocusManager.current
    var draft by remember { mutableStateOf(location.relativePath) }
    LaunchedEffect(location.relativePath) {
        draft = location.relativePath
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("修改后的保存位置", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        Text(
            location.displayLabel(),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.primary,
        )
        if (location.treeUri != null) {
            Text(
                "现在用的是你选的文件夹。改用下面的相册路径后，就会取消它。",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
            )
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("相册里的文件夹", style = MaterialTheme.typography.titleSmall) },
            placeholder = { Text("Pictures/NeriExifTools/Edited", style = MaterialTheme.typography.bodyLarge) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onApplyPath(draft)
            }),
            colors = colors,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(18.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onApplyPath(draft) },
                enabled = enabled,
                border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.7f)),
            ) {
                Text("用这个路径", color = scheme.onSurface)
            }
            OutlinedButton(
                onClick = onPickFolder,
                enabled = enabled,
                border = BorderStroke(1.dp, scheme.outline),
            ) {
                Text("选择文件夹", color = scheme.onSurface)
            }
        }
        OutlinedButton(
            onClick = onReset,
            enabled = enabled,
            modifier = Modifier.padding(bottom = 4.dp),
            border = BorderStroke(1.dp, scheme.outline),
        ) {
            Text("恢复默认位置", color = scheme.onSurface)
        }
    }
}
