package com.neri.exiftools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * ColorOS 相册 Picker URI 不支持 requireOriginal。
 * 优先 GET_CONTENT / OPEN_DOCUMENT，避免系统自己带上 ?requireOriginal=1。
 */
@Composable
fun rememberImagePicker(
    onPicked: (Uri) -> Unit,
    onFailed: (String) -> Unit,
): () -> Unit {
    val getContent = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) onPicked(uri)
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onPicked(uri)
    }

    return remember(getContent, openDocument) {
        {
            try {
                getContent.launch("image/*")
            } catch (_: Exception) {
                try {
                    openDocument.launch(arrayOf("image/jpeg", "image/png", "image/webp", "image/*"))
                } catch (error: Exception) {
                    onFailed(error.message ?: "音理打不开相册，看看有没有图库或文件应用")
                }
            }
        }
    }
}
