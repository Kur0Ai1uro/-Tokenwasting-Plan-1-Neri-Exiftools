package com.neri.exiftools.ui.detail

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neri.exiftools.model.CommonExifFields
import com.neri.exiftools.model.CustomField
import com.neri.exiftools.model.ImageFormat
import com.neri.exiftools.model.ImageInfo
import com.neri.exiftools.model.TagGroup
import com.neri.exiftools.model.TagItem
import com.neri.exiftools.ui.theme.OnriAvatar
import com.neri.exiftools.ui.theme.OnriBowMark
import com.neri.exiftools.ui.theme.OnriGlassCard
import com.neri.exiftools.ui.theme.OnriSectionTitle
import com.neri.exiftools.ui.theme.OnriStage
import com.neri.exiftools.ui.theme.onriTextFieldColors
import com.neri.exiftools.ui.SaveLocationSection
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    info: ImageInfo,
    preview: Bitmap?,
    fields: CommonExifFields,
    customFields: List<CustomField>,
    tagGroups: List<TagGroup>,
    tagSearch: String,
    errorMessage: String?,
    isSaving: Boolean,
    isLoading: Boolean,
    hasChanges: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onClearGps: () -> Unit,
    onRestore: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFieldsChange: ((CommonExifFields) -> CommonExifFields) -> Unit,
    onAddCustomField: (String, String) -> Unit,
    onUpdateCustomField: (String, String) -> Unit,
    onRemoveCustomField: (String) -> Unit,
    saveLocation: com.neri.exiftools.util.SaveLocation,
    onApplySavePath: (String) -> Unit,
    onPickSaveFolder: () -> Unit,
    onResetSaveLocation: () -> Unit,
    onSaveCopy: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val filteredGroups = remember(tagGroups, tagSearch) { filterGroups(tagGroups, tagSearch) }
    val fieldColors = onriTextFieldColors()
    val scheme = MaterialTheme.colorScheme
    val canSave = hasChanges && !isSaving && !isLoading
    val hasGps = fields.gpsLatitude.isNotBlank() ||
        fields.gpsLongitude.isNotBlank() ||
        fields.gpsAltitude.isNotBlank()
    val decimalKeyboard = KeyboardOptions(
        keyboardType = KeyboardType.Decimal,
        imeAction = ImeAction.Next,
    )
    val saveEdits = {
        focusManager.clearFocus()
        onSave()
    }
    val previewBitmap = remember(preview) {
        preview?.takeUnless { it.isRecycled }?.asImageBitmap()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OnriAvatar(size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                info.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                if (hasChanges) "音理记下了还没保存的改动" else "音理正在看这份记录",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onSaveCopy, enabled = !isSaving && !isLoading) {
                        Text("保存", color = if (!isSaving && !isLoading) scheme.primary else scheme.outline)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        OnriStage(
            modifier = Modifier.padding(innerPadding),
            mascotAlpha = 0.22f,
            showDecor = true,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    PreviewHero(
                        preview = previewBitmap,
                        title = info.displayName,
                        subtitle = "${info.width} × ${info.height}  ·  ${info.format.displayName}  ·  ${formatSize(info.sizeBytes)}",
                        note = if (info.format != ImageFormat.JPEG) {
                            "PNG/WebP 能写回去的字段少一点，保存后再打开看一眼就好。"
                        } else {
                            null
                        },
                    )
                }
                if (!errorMessage.isNullOrBlank()) {
                    item {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                item(key = "common-fields") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OnriSectionTitle("常用字段")
                        ExifTextField("拍摄时间", fields.dateTimeOriginal, "yyyy:MM:dd HH:mm:ss", fieldColors) {
                            onFieldsChange { current -> current.copy(dateTimeOriginal = it) }
                        }
                        ExifTextField("时区偏移", fields.offsetTimeOriginal, "+08:00", fieldColors) {
                            onFieldsChange { current -> current.copy(offsetTimeOriginal = it) }
                        }
                        ExifTextField("相机制造商", fields.make, colors = fieldColors) {
                            onFieldsChange { current -> current.copy(make = it) }
                        }
                        ExifTextField("相机型号", fields.model, colors = fieldColors) {
                            onFieldsChange { current -> current.copy(model = it) }
                        }
                        ExifTextField("镜头型号", fields.lensModel, colors = fieldColors) {
                            onFieldsChange { current -> current.copy(lensModel = it) }
                        }
                        ExifTextField(
                            label = "描述",
                            value = fields.imageDescription,
                            colors = fieldColors,
                            singleLine = false,
                            minLines = 3,
                            maxLines = 6,
                        ) {
                            onFieldsChange { current -> current.copy(imageDescription = it) }
                        }
                        ExifTextField("版权", fields.copyright, colors = fieldColors) {
                            onFieldsChange { current -> current.copy(copyright = it) }
                        }
                        ExifTextField(
                            label = "用户注释",
                            value = fields.userComment,
                            colors = fieldColors,
                            singleLine = false,
                            minLines = 3,
                            maxLines = 8,
                        ) {
                            onFieldsChange { current -> current.copy(userComment = it) }
                        }
                        OrientationField(fields.orientation, fieldColors) { value ->
                            onFieldsChange { current -> current.copy(orientation = value) }
                        }
                        ExifTextField(
                            label = "纬度",
                            value = fields.gpsLatitude,
                            placeholder = "南纬写成负数就可以啦",
                            colors = fieldColors,
                            keyboardOptions = decimalKeyboard,
                        ) {
                            onFieldsChange { current -> current.copy(gpsLatitude = it) }
                        }
                        ExifTextField(
                            label = "经度",
                            value = fields.gpsLongitude,
                            placeholder = "西经写成负数就可以啦",
                            colors = fieldColors,
                            keyboardOptions = decimalKeyboard,
                        ) {
                            onFieldsChange { current -> current.copy(gpsLongitude = it) }
                        }
                        ExifTextField(
                            label = "海拔（米）",
                            value = fields.gpsAltitude,
                            colors = fieldColors,
                            keyboardOptions = decimalKeyboard,
                        ) {
                            onFieldsChange { current -> current.copy(gpsAltitude = it) }
                        }
                        CustomFieldsSection(
                            fields = customFields,
                            enabled = !isSaving && !isLoading,
                            colors = fieldColors,
                            onAdd = onAddCustomField,
                            onValueChange = onUpdateCustomField,
                            onRemove = onRemoveCustomField,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onClearGps,
                                enabled = !isSaving && hasGps,
                                border = BorderStroke(1.dp, scheme.outline),
                            ) {
                                Text("清除 GPS", color = scheme.onSurface)
                            }
                            OutlinedButton(
                                onClick = onRestore,
                                enabled = !isSaving && hasChanges,
                                border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.7f)),
                            ) {
                                Text("恢复原值", color = scheme.onSurface)
                            }
                        }
                        if (isSaving || isLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = scheme.primary,
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    if (isSaving) "音理正在小心写下…" else "音理正在翻看…",
                                    color = scheme.onSurface,
                                )
                            }
                        }
                        SaveLocationSection(
                            location = saveLocation,
                            enabled = !isSaving && !isLoading,
                            onApplyPath = onApplySavePath,
                            onPickFolder = onPickSaveFolder,
                            onReset = onResetSaveLocation,
                        )
                        Text(
                            "保存到所选位置不会改原图。覆盖原图会先做备份，只有确认是同一张照片才会写回去。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                        )
                        Button(
                            onClick = onSaveCopy,
                            enabled = !isSaving && !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.primary,
                                contentColor = scheme.onPrimary,
                            ),
                        ) {
                            OnriBowMark(modifier = Modifier.size(16.dp), color = scheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("保存到所选位置")
                        }
                        OutlinedButton(
                            onClick = saveEdits,
                            enabled = canSave,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, scheme.outline),
                        ) {
                            Text("覆盖原图", color = scheme.onSurface)
                        }
                    }
                }
                item(key = "all-tags") { OnriSectionTitle("全部标签") }
                item(key = "tag-search") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = tagSearch,
                            onValueChange = onSearchChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("搜索标签名或值", style = MaterialTheme.typography.titleSmall) },
                            colors = fieldColors,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            shape = RoundedCornerShape(18.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                        TextButton(
                            onClick = {
                                val text = formatAllTags(filteredGroups)
                                if (text.isBlank()) {
                                    Toast.makeText(context, "这里还没有标签哦", Toast.LENGTH_SHORT).show()
                                } else {
                                    clipboard.setText(AnnotatedString(text))
                                    Toast.makeText(context, "音理帮你复制好了", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Text("复制全部")
                        }
                    }
                }
                filteredGroups.forEach { group ->
                    val isExpanded = tagSearch.isNotBlank() || expanded[group.directoryName] == true
                    item(key = "dir-${group.directoryName}") {
                        OnriGlassCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded[group.directoryName] = !isExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(group.directoryName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${group.tags.size} 项",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = scheme.primary,
                                )
                            }
                        }
                    }
                    if (isExpanded) {
                        items(
                            items = group.tags,
                            key = { tag -> "${group.directoryName}|${tag.name}|${tag.value}" },
                        ) { tag ->
                            TagRow(tag) {
                                clipboard.setText(AnnotatedString("${tag.name}: ${tag.value}"))
                                Toast.makeText(context, "已经复制好啦", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun PreviewHero(
    preview: ImageBitmap?,
    title: String,
    subtitle: String,
    note: String?,
) {
    val scheme = MaterialTheme.colorScheme
    if (preview != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(248.dp)
                .clip(MaterialTheme.shapes.large),
        ) {
            Image(
                bitmap = preview,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, scheme.secondary.copy(alpha = 0.82f)),
                        ),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSecondary.copy(alpha = 0.9f))
                    if (!note.isNullOrBlank()) {
                        Text(note, style = MaterialTheme.typography.bodySmall, color = scheme.onSecondary.copy(alpha = 0.86f))
                    }
                }
            }
        }
    } else {
        OnriGlassCard {
            Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            if (!note.isNullOrBlank()) {
                Text(note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            }
        }
    }
}

@Composable
private fun ExifTextField(
    label: String,
    value: String,
    placeholder: String? = null,
    colors: TextFieldColors,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, style = MaterialTheme.typography.titleSmall) },
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = if (singleLine) keyboardOptions else KeyboardOptions.Default,
        colors = colors,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(18.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrientationField(
    value: String,
    colors: TextFieldColors,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("" to "未设置") + (1..8).map { it.toString() to orientationLabel(it) }
    val selected = options.firstOrNull { it.first == value } ?: (value to value)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.second,
            onValueChange = {},
            readOnly = true,
            label = { Text("方向", style = MaterialTheme.typography.titleSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = colors,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(key)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagRow(tag: TagItem, onCopy: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface.copy(alpha = 0.62f))
            .combinedClickable(onClick = onCopy, onLongClick = onCopy)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(tag.name, style = MaterialTheme.typography.titleSmall, color = scheme.primary)
        Text(
            tag.value,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface,
        )
    }
}

private fun filterGroups(groups: List<TagGroup>, query: String): List<TagGroup> {
    val needle = query.trim()
    if (needle.isEmpty()) return groups
    return groups.mapNotNull { group ->
        val tags = group.tags.filter { tag ->
            tag.name.contains(needle, ignoreCase = true) ||
                tag.value.contains(needle, ignoreCase = true) ||
                group.directoryName.contains(needle, ignoreCase = true)
        }
        if (tags.isEmpty()) null else group.copy(tags = tags)
    }
}

private fun formatAllTags(groups: List<TagGroup>): String {
    return groups.joinToString("\n\n") { group ->
        buildString {
            append("# ${group.directoryName}")
            group.tags.forEach { tag ->
                append('\n')
                append(tag.name)
                append(": ")
                append(tag.value)
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.2f MB", kb / 1024.0)
}

private fun orientationLabel(value: Int): String = when (value) {
    1 -> "1 · 正常"
    2 -> "2 · 水平镜像"
    3 -> "3 · 旋转 180°"
    4 -> "4 · 垂直镜像"
    5 -> "5 · 镜像并顺时针 90°"
    6 -> "6 · 顺时针 90°"
    7 -> "7 · 镜像并逆时针 90°"
    8 -> "8 · 逆时针 90°"
    else -> value.toString()
}
