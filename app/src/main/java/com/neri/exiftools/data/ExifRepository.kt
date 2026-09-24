package com.neri.exiftools.data

import android.content.Context
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import com.neri.exiftools.model.CommonExifFields
import com.neri.exiftools.model.CustomField
import com.neri.exiftools.model.ImageFormat
import com.neri.exiftools.model.LoadedImage
import com.neri.exiftools.util.FieldValidator
import com.neri.exiftools.util.SaveLocation
import java.io.File
import java.io.IOException

class ExifRepository(context: Context) {
    private val appContext = context.applicationContext
    private val reader = MetadataReader()
    private val writer = MetadataWriter()
    private val backupManager = BackupManager(appContext)
    private val mediaWriteHelper = MediaWriteHelper(appContext)
    private val imageLoader = ImageLoader(appContext)

    fun load(uri: Uri): LoadedImage {
        imageLoader.cleanupStaleCache()
        val (displayName, sizeBytes) = imageLoader.queryDisplayName(uri)
        val workingFile = imageLoader.copyToCache(uri, displayName)
        val info = imageLoader.inspect(uri, workingFile, displayName, sizeBytes)
        if (info.format == ImageFormat.UNSUPPORTED) {
            workingFile.delete()
            throw UnsupportedFormatException("音理只看得懂 JPEG、PNG、WebP 哦")
        }
        val snapshot = reader.read(workingFile)
        val preview = imageLoader.decodePreview(workingFile, info.width, info.height)
        return LoadedImage(
            info = info,
            fields = snapshot.fields,
            groups = snapshot.groups,
            customFields = snapshot.customFields,
            workingFile = workingFile,
            preview = preview,
        )
    }

    fun prepareSave(
        sourceUri: Uri,
        workingFile: File,
        displayName: String,
        format: ImageFormat,
        fields: CommonExifFields,
        customFields: List<CustomField> = emptyList(),
        tagsToClear: Set<String> = emptySet(),
    ): PrepareSaveResult {
        val errors = FieldValidator.validate(fields, customFields)
        if (errors.isNotEmpty()) {
            return PrepareSaveResult.ValidationError(errors)
        }

        val backup = try {
            backupManager.backup(workingFile, displayName, format)
        } catch (error: Exception) {
            return PrepareSaveResult.Error("备份失败：${error.message ?: "未知错误"}")
        }

        val edited = File(workingFile.parentFile, "neri_edited_${System.currentTimeMillis()}.${format.extension}")
        try {
            copyFast(workingFile, edited)
            writer.write(edited, fields, customFields, tagsToClear)
        } catch (error: Exception) {
            edited.delete()
            return PrepareSaveResult.Error("写入失败：${error.message?.let(MediaUris::userFacing) ?: "未知错误"}")
        }

        val resolved = mediaWriteHelper.resolveMediaStoreUri(sourceUri)
        val mediaUri = when {
            resolved != null && !MediaUris.isReadOnlyGalleryUri(resolved) -> resolved
            !MediaUris.isReadOnlyGalleryUri(sourceUri) -> sourceUri
            else -> null
        }
        if (mediaUri == null) {
            return PrepareSaveResult.NeedSaveAs(
                editedFile = edited,
                backupName = backup.displayName,
                reason = MediaUris.READ_ONLY_MESSAGE,
            )
        }
        when (val overwrite = mediaWriteHelper.overwrite(mediaUri, edited)) {
            MediaWriteHelper.OverwriteResult.Success -> {
                copyFast(edited, workingFile)
                val saved = reader.read(workingFile)
                edited.delete()
                return PrepareSaveResult.Overwritten(
                    backupName = backup.displayName,
                    workingFile = workingFile,
                    fields = saved.fields,
                    groups = saved.groups,
                    customFields = saved.customFields,
                )
            }

            is MediaWriteHelper.OverwriteResult.NeedUserConsent -> {
                return PrepareSaveResult.NeedWritePermission(
                    request = IntentSenderRequest.Builder(overwrite.intentSender).build(),
                    editedFile = edited,
                    destination = mediaUri,
                    backupName = backup.displayName,
                )
            }

            is MediaWriteHelper.OverwriteResult.Denied -> {
                val request = mediaWriteHelper.createWriteRequest(mediaUri)
                if (request != null) {
                    return PrepareSaveResult.NeedWritePermission(
                        request = request,
                        editedFile = edited,
                        destination = mediaUri,
                        backupName = backup.displayName,
                    )
                }
                return PrepareSaveResult.NeedSaveAs(
                    editedFile = edited,
                    backupName = backup.displayName,
                    reason = overwrite.reason,
                )
            }
        }
    }

    fun finishOverwrite(
        destination: Uri,
        editedFile: File,
        workingFile: File,
        backupName: String,
    ): PrepareSaveResult {
        return try {
            mediaWriteHelper.writeToUri(destination, editedFile)
            copyFast(editedFile, workingFile)
            val saved = reader.read(workingFile)
            editedFile.delete()
            PrepareSaveResult.Overwritten(
                backupName = backupName,
                workingFile = workingFile,
                fields = saved.fields,
                groups = saved.groups,
                customFields = saved.customFields,
            )
        } catch (error: Exception) {
            PrepareSaveResult.NeedSaveAs(
                editedFile = editedFile,
                backupName = backupName,
                reason = MediaUris.userFacing(error.message),
            )
        }
    }

    fun saveCopy(
        workingFile: File,
        displayName: String,
        format: ImageFormat,
        fields: CommonExifFields,
        customFields: List<CustomField> = emptyList(),
        tagsToClear: Set<String> = emptySet(),
        location: SaveLocation,
    ): PrepareSaveResult {
        val errors = FieldValidator.validate(fields, customFields)
        if (errors.isNotEmpty()) {
            return PrepareSaveResult.ValidationError(errors)
        }
        val backup = try {
            backupManager.backup(workingFile, displayName, format)
        } catch (error: Exception) {
            return PrepareSaveResult.Error("备份失败：${error.message ?: "未知错误"}")
        }
        val edited = File(workingFile.parentFile, "neri_edited_${System.currentTimeMillis()}.${format.extension}")
        try {
            copyFast(workingFile, edited)
            writer.write(edited, fields, customFields, tagsToClear)
        } catch (error: Exception) {
            edited.delete()
            return PrepareSaveResult.Error("写入失败：${error.message?.let(MediaUris::userFacing) ?: "未知错误"}")
        }
        return saveAs(edited, workingFile, displayName, format, backup.displayName, location)
    }

    fun saveAs(
        editedFile: File,
        workingFile: File,
        displayName: String,
        format: ImageFormat,
        backupName: String,
        location: SaveLocation = SaveLocation(),
    ): PrepareSaveResult {
        return try {
            val savedCopy = backupManager.saveEditedCopy(editedFile, displayName, format, location)
            copyFast(editedFile, workingFile)
            val saved = reader.read(workingFile)
            editedFile.delete()
            PrepareSaveResult.SavedAs(
                backupName = backupName,
                newName = savedCopy.displayName,
                savedPath = savedCopy.savedPath.ifBlank { location.displayLabel() },
                workingFile = workingFile,
                fields = saved.fields,
                groups = saved.groups,
                customFields = saved.customFields,
            )
        } catch (error: Exception) {
            PrepareSaveResult.Error("另存失败：${error.message ?: "未知错误"}")
        }
    }

    class UnsupportedFormatException(message: String) : IOException(message)
}

private fun copyFast(source: File, target: File) {
    source.inputStream().use { input ->
        target.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
    }
}

sealed class PrepareSaveResult {
    data class Overwritten(
        val backupName: String,
        val workingFile: File,
        val fields: com.neri.exiftools.model.CommonExifFields,
        val groups: List<com.neri.exiftools.model.TagGroup>,
        val customFields: List<CustomField> = emptyList(),
    ) : PrepareSaveResult()

    data class SavedAs(
        val backupName: String,
        val newName: String,
        val savedPath: String = "",
        val workingFile: File,
        val fields: com.neri.exiftools.model.CommonExifFields,
        val groups: List<com.neri.exiftools.model.TagGroup>,
        val customFields: List<CustomField> = emptyList(),
    ) : PrepareSaveResult()

    data class NeedWritePermission(
        val request: IntentSenderRequest,
        val editedFile: File,
        val destination: Uri,
        val backupName: String,
    ) : PrepareSaveResult()

    data class NeedSaveAs(
        val editedFile: File,
        val backupName: String,
        val reason: String,
    ) : PrepareSaveResult()

    data class ValidationError(val errors: List<String>) : PrepareSaveResult()
    data class Error(val message: String) : PrepareSaveResult()
}
