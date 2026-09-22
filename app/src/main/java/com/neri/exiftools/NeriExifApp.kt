package com.neri.exiftools

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neri.exiftools.ui.ExifViewModel
import com.neri.exiftools.ui.detail.DetailScreen
import com.neri.exiftools.ui.home.HomeScreen
import com.neri.exiftools.ui.theme.LocalOnriMascot
import com.neri.exiftools.ui.theme.rememberOnriMascotBitmap

@Composable
fun NeriExifApp(viewModel: ExifViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscard by remember { mutableStateOf(false) }
    val latestState = rememberUpdatedState(state)
    val mascot = rememberOnriMascotBitmap()

    val pickImage = rememberImagePicker(
        onPicked = viewModel::loadImage,
        onFailed = viewModel::showMessage,
    )

    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onWritePermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.save()
        } else {
            viewModel.showMessage("没有存储权限，音理没法写回去")
        }
    }

    val requestClose = {
        val current = latestState.value
        when {
            current.isSaving -> Unit
            current.hasChanges -> showDiscard = true
            else -> viewModel.closeImage()
        }
    }

    val requestSave = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.save()
        }
    }

    BackHandler(enabled = state.hasImage) { requestClose() }

    LaunchedEffect(state.snackbarMessage) {
        val message = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeSnackbar()
    }

    LaunchedEffect(state.writeRequest) {
        val request = state.writeRequest ?: return@LaunchedEffect
        writePermissionLauncher.launch(request)
        viewModel.consumeWriteRequest()
    }

    CompositionLocalProvider(LocalOnriMascot provides mascot) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.hasImage && state.imageInfo != null) {
                DetailScreen(
                    info = state.imageInfo!!,
                    preview = state.preview,
                    fields = state.fields,
                    customFields = state.customFields,
                    tagGroups = state.tagGroups,
                    tagSearch = state.tagSearch,
                    errorMessage = state.errorMessage,
                    isSaving = state.isSaving,
                    isLoading = state.isLoading,
                    hasChanges = state.hasChanges,
                    onBack = { requestClose() },
                    onSave = requestSave,
                    onClearGps = viewModel::clearGps,
                    onRestore = viewModel::restoreOriginal,
                    onSearchChange = viewModel::updateSearch,
                    onFieldsChange = viewModel::updateFields,
                    onAddCustomField = viewModel::addCustomField,
                    onUpdateCustomField = viewModel::updateCustomField,
                    onRemoveCustomField = viewModel::removeCustomField,
                )
            } else {
                HomeScreen(
                    isLoading = state.isLoading,
                    onPickImage = pickImage,
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
            ) { data ->
                val scheme = MaterialTheme.colorScheme
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(20.dp),
                    containerColor = scheme.secondary,
                    contentColor = scheme.onSecondary,
                    actionColor = scheme.primary,
                )
            }
        }
    }

    val scheme = MaterialTheme.colorScheme
    state.saveAsPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = viewModel::cancelSaveAs,
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            textContentColor = scheme.onSurfaceVariant,
            shape = RoundedCornerShape(28.dp),
            title = { Text("音理没能改到原图") },
            text = {
                Text("${prompt.reason}\n原图已经备份为 ${prompt.backupName}。要不要让音理另存一份？")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSaveAs) { Text("另存为新图") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelSaveAs) { Text("取消") }
            },
        )
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            textContentColor = scheme.onSurfaceVariant,
            shape = RoundedCornerShape(28.dp),
            title = { Text("还没保存哦") },
            text = { Text("音理还没把改动写回去。要丢掉这些修改吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscard = false
                        viewModel.closeImage()
                    },
                ) { Text("丢掉修改") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) { Text("继续编辑") }
            },
        )
    }
}
