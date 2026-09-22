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
    onAdd: (String) -> Unit,
    onValueChange: (String, String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OnriSectionTitle("自定义字段")
        Text(
            text = "挑一个音理能写回去的字段，再填上你想要的值。留空保存就会清掉它。",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
        )
        fields.forEach { field ->
            val spec = WritableTagCatalog.find(field.tag) ?: return@forEach
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = field.value,
                    onValueChange = { onValueChange(field.tag, it) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    label = { Text(spec.label, style = MaterialTheme.typography.titleSmall) },
                    placeholder = spec.hint.takeIf { it.isNotBlank() }?.let {
                        { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    },
                    supportingText = { Text(spec.tag, style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when (spec.kind) {
                            TagValueKind.TEXT -> KeyboardType.Text
                            TagValueKind.INTEGER -> KeyboardType.Number
                            TagValueKind.RATIONAL -> KeyboardType.Decimal
                        },
                    ),
                    colors = colors,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(18.dp),
                )
                IconButton(onClick = { onRemove(field.tag) }, enabled = enabled) {
                    Icon(Icons.Outlined.Close, contentDescription = "移除${spec.label}", tint = scheme.primary)
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
            onAdd = { tag ->
                onAdd(tag)
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
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val available = remember(usedTags, query) {
        WritableTagCatalog.tags.filter { spec ->
            spec.tag !in usedTags && (
                query.isBlank() ||
                    spec.label.contains(query.trim(), ignoreCase = true) ||
                    spec.tag.contains(query.trim(), ignoreCase = true)
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
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("搜索字段", style = MaterialTheme.typography.titleSmall) },
                    colors = colors,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(18.dp),
                )
                if (available.isEmpty()) {
                    Text(
                        if (usedTags.size == WritableTagCatalog.tags.size) "能加的字段都在上面啦" else "没有找到这个字段",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        available.forEach { spec ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAdd(spec.tag) }
                                    .padding(vertical = 10.dp),
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
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
