package com.neri.exiftools.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neri.exiftools.model.CustomField
import com.neri.exiftools.ui.theme.OnriSectionTitle
import com.neri.exiftools.util.TagValueKind
import com.neri.exiftools.util.WritableTagCatalog

@Composable
fun CustomFieldsSection(
    fields: List<CustomField>,
    enabled: Boolean,
    colors: TextFieldColors,
    onAdd: (String, String) -> Unit,
    onValueChange: (String, String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OnriSectionTitle("自定义字段")
        Text(
            text = "对得上音理认识的名字，会写成标准字段，别的软件也能看见。自己起的名字也能保存，但多半只有音理打开才看得到。内容留空再保存就会清掉。",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
        )
        fields.forEach { field ->
            val spec = WritableTagCatalog.resolve(field.tag)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = field.value,
                    onValueChange = { onValueChange(field.tag, it) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    label = { Text(spec?.label ?: field.tag, style = MaterialTheme.typography.titleSmall) },
                    placeholder = spec?.hint?.takeIf { it.isNotBlank() }?.let {
                        { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    },
                    supportingText = {
                        Text(
                            if (spec != null) "标准字段 · 别的软件也能看见" else "自定义 · 多半只有音理能看见",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when (spec?.kind) {
                            TagValueKind.INTEGER -> KeyboardType.Number
                            TagValueKind.RATIONAL -> KeyboardType.Decimal
                            else -> KeyboardType.Text
                        },
                    ),
                    colors = colors,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(18.dp),
                )
                IconButton(onClick = { onRemove(field.tag) }, enabled = enabled) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "移除${spec?.label ?: field.tag}",
                        tint = scheme.primary,
                    )
                }
            }
        }
        OutlinedButton(
            onClick = { showPicker = true },
            enabled = enabled,
            border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.7f)),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = scheme.primary)
            Text("添加字段", color = scheme.onSurface, modifier = Modifier.padding(start = 6.dp))
        }
    }
    if (showPicker) {
        AddFieldDialog(
            usedTags = fields.map { it.tag }.toSet(),
            colors = colors,
            onAdd = { name, value ->
                onAdd(name, value)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun AddFieldDialog(
    usedTags: Set<String>,
    colors: TextFieldColors,
    onAdd: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val suggestions = remember(usedTags, name) {
        WritableTagCatalog.tags.filter { spec ->
            spec.tag !in usedTags && (
                name.isBlank() ||
                    spec.label.contains(name.trim(), ignoreCase = true) ||
                    spec.tag.contains(name.trim(), ignoreCase = true)
                )
        }
    }
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surface,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurface,
        shape = RoundedCornerShape(28.dp),
        title = { Text("添加字段") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("字段名", style = MaterialTheme.typography.titleSmall) },
                    placeholder = { Text("比如 作者，或自己起一个名字", style = MaterialTheme.typography.bodyLarge) },
                    colors = colors,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(18.dp),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("字段内容", style = MaterialTheme.typography.titleSmall) },
                    colors = colors,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(18.dp),
                )
                if (!error.isNullOrBlank()) {
                    Text(error.orEmpty(), color = scheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                if (suggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        suggestions.forEach { spec ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { name = spec.label }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(spec.label, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                Text(
                                    listOf(spec.tag, spec.hint).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        error = "先写字段名"
                    } else if (WritableTagCatalog.resolve(name)?.tag in usedTags || name.trim() in usedTags) {
                        error = "这个字段已经有了"
                    } else {
                        onAdd(name.trim(), value)
                    }
                },
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
