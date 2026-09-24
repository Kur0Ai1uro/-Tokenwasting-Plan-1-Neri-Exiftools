package com.neri.exiftools.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neri.exiftools.data.ExifRepository
import com.neri.exiftools.data.PrepareSaveResult
import com.neri.exiftools.data.SaveLocationStore
import com.neri.exiftools.model.CommonExifFields
import com.neri.exiftools.model.CustomField
import com.neri.exiftools.model.ImageInfo
import com.neri.exiftools.model.TagGroup
import com.neri.exiftools.util.SaveLocation
import com.neri.exiftools.util.SavePathRules
import com.neri.exiftools.util.WritableTagCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ExifUiState(
    val hasImage: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val imageInfo: ImageInfo? = null,
    val preview: Bitmap? = null,
    val fields: CommonExifFields = CommonExifFields(),
    val originalFields: CommonExifFields = CommonExifFields(),
    val customFields: List<CustomField> = emptyList(),
    val originalCustomFields: List<CustomField> = emptyList(),
    val tagGroups: List<TagGroup> = emptyList(),
    val tagSearch: String = "",
    val snackbarMessage: String? = null,
    val errorMessage: String? = null,
    val writeRequest: IntentSenderRequest? = null,
    val saveAsPrompt: SaveAsPrompt? = null,
    val saveLocation: SaveLocation = SaveLocation(),
) {
    val hasChanges: Boolean
        get() = hasImage && (
            fields != originalFields ||
                customFields.customValueMap() != originalCustomFields.customValueMap()
            )
}

data class SaveAsPrompt(
    val reason: String,
    val backupName: String,
)

class ExifViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExifRepository(application)
    private val locationStore = SaveLocationStore(application)

    private val _uiState = MutableStateFlow(ExifUiState(saveLocation = locationStore.load()))
    val uiState: StateFlow<ExifUiState> = _uiState.asStateFlow()

    private var workingFile: File? = null
    private var pendingEditedFile: File? = null
    private var pendingDestination: Uri? = null
    private var pendingBackupName: String? = null
    private var loadJob: Job? = null

    fun showMessage(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun loadImage(uri: Uri) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val previousPreview = _uiState.value.preview
            try {
                val loaded = withContext(Dispatchers.IO) {
                    persistUriAccess(uri)
                    repository.load(uri)
                }
                workingFile?.delete()
                workingFile = loaded.workingFile
                _uiState.update {
                    it.copy(
                        hasImage = true,
                        isLoading = false,
                        imageInfo = loaded.info,
                        preview = loaded.preview,
                        fields = loaded.fields,
                        originalFields = loaded.fields,
                        customFields = loaded.customFields,
                        originalCustomFields = loaded.customFields,
                        tagGroups = loaded.groups,
                        tagSearch = "",
                        errorMessage = null,
                    )
                }
                recycleLater(previousPreview, loaded.preview)
            } catch (error: CancellationException) {
                throw error
            } catch (error: ExifRepository.UnsupportedFormatException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message,
                        snackbarMessage = error.message,
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackbarMessage = "音理打不开这张图，换一张试试吧",
                    )
                }
            }
        }
    }

    fun updateFields(transform: (CommonExifFields) -> CommonExifFields) {
        _uiState.update { it.copy(fields = transform(it.fields)) }
    }

    fun addCustomField(name: String, value: String) {
        val spec = WritableTagCatalog.resolve(name)
        val tag = spec?.tag ?: name.trim()
        if (tag.isEmpty()) return
        _uiState.update { state ->
            val exists = state.customFields.any { field ->
                field.tag.equals(tag, ignoreCase = true) ||
                    WritableTagCatalog.resolve(field.tag)?.tag == tag
            }
            if (exists) state
            else state.copy(customFields = state.customFields + CustomField(tag, value))
        }
    }

    fun updateCustomField(tag: String, value: String) {
        _uiState.update { state ->
            state.copy(
                customFields = state.customFields.map { field ->
                    if (field.tag == tag) field.copy(value = value) else field
                },
            )
        }
    }

    fun removeCustomField(tag: String) {
        _uiState.update { state ->
            state.copy(customFields = state.customFields.filterNot { it.tag == tag })
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(tagSearch = query) }
    }

    fun clearGps() {
        _uiState.update {
            it.copy(
                fields = it.fields.copy(
                    gpsLatitude = "",
                    gpsLongitude = "",
                    gpsAltitude = "",
                ),
            )
        }
    }

    fun restoreOriginal() {
        _uiState.update {
            it.copy(
                fields = it.originalFields,
                customFields = it.originalCustomFields,
            )
        }
    }

    fun closeImage() {
        loadJob?.cancel()
        val preview = _uiState.value.preview
        val location = _uiState.value.saveLocation
        workingFile?.delete()
        workingFile = null
        pendingEditedFile?.delete()
        pendingEditedFile = null
        _uiState.value = ExifUiState(saveLocation = location)
        recycleLater(preview, keep = null)
    }

    fun applySavePath(raw: String) {
        val normalized = SavePathRules.normalize(raw)
        if (normalized == null) {
            showMessage("路径里不能有 ..")
            return
        }
        locationStore.saveRelative(normalized)
        publishSaveLocation()
        showMessage("修改后的照片会存到 $normalized")
    }

    fun useSaveTree(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
        }
        locationStore.saveTree(uri.toString())
        val label = publishSaveLocation().displayLabel()
        showMessage("音理会把修改后的照片放进 $label")
    }

    fun resetSaveLocation() {
        locationStore.reset()
        publishSaveLocation()
        showMessage("已经改回默认位置")
    }

    fun saveToChosenLocation() {
        val current = _uiState.value
        val info = current.imageInfo ?: return
        val file = workingFile ?: return
        val customFields = current.customFields
        val tagsToClear = current.originalCustomFields
            .map { it.tag }
            .filter { tag -> customFields.none { it.tag == tag } }
            .toSet()
        val location = locationStore.load()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = withContext(Dispatchers.IO) {
                repository.saveCopy(
                    workingFile = file,
                    displayName = info.displayName,
                    format = info.format,
                    fields = _uiState.value.fields,
                    customFields = customFields,
                    tagsToClear = tagsToClear,
                    location = location,
                )
            }
            handleSaveResult(result)
        }
    }

    fun save() {
        val current = _uiState.value
        if (!current.hasChanges) return
        val info = current.imageInfo ?: return
        val file = workingFile ?: return
        val customFields = current.customFields
        val tagsToClear = current.originalCustomFields
            .map { it.tag }
            .filter { tag -> customFields.none { it.tag == tag } }
            .toSet()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = withContext(Dispatchers.IO) {
                repository.prepareSave(
                    sourceUri = info.uri,
                    workingFile = file,
                    displayName = info.displayName,
                    format = info.format,
                    fields = _uiState.value.fields,
                    customFields = customFields,
                    tagsToClear = tagsToClear,
                )
            }
            handleSaveResult(result)
        }
    }

    fun onWritePermissionResult(granted: Boolean) {
        val edited = pendingEditedFile
        val destination = pendingDestination
        val backupName = pendingBackupName
        val file = workingFile
        _uiState.update { it.copy(writeRequest = null) }
        if (!granted || edited == null || destination == null || backupName == null || file == null) {
            if (edited != null && destination != null && backupName != null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveAsPrompt = SaveAsPrompt(
                            reason = "未获得覆盖原图的授权",
                            backupName = backupName,
                        ),
                    )
                }
            } else {
                _uiState.update { it.copy(isSaving = false) }
            }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.finishOverwrite(destination, edited, file, backupName)
            }
            handleSaveResult(result)
        }
    }

    fun confirmSaveAs() {
        val edited = pendingEditedFile ?: return
        val file = workingFile ?: return
        val info = _uiState.value.imageInfo ?: return
        val backupName = pendingBackupName ?: _uiState.value.saveAsPrompt?.backupName.orEmpty()
        val location = locationStore.load()
        viewModelScope.launch {
            _uiState.update { it.copy(saveAsPrompt = null, isSaving = true) }
            val result = withContext(Dispatchers.IO) {
                repository.saveAs(
                    editedFile = edited,
                    workingFile = file,
                    displayName = info.displayName,
                    format = info.format,
                    backupName = backupName,
                    location = location,
                )
            }
            handleSaveResult(result)
        }
    }

    fun cancelSaveAs() {
        pendingEditedFile?.delete()
        pendingEditedFile = null
        pendingDestination = null
        pendingBackupName = null
        _uiState.update { it.copy(saveAsPrompt = null, isSaving = false) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun consumeWriteRequest() {
        _uiState.update { it.copy(writeRequest = null) }
    }

    private fun publishSaveLocation(): SaveLocation {
        val location = locationStore.load()
        _uiState.update { it.copy(saveLocation = location) }
        return location
    }

    private fun handleSaveResult(result: PrepareSaveResult) {
        when (result) {
            is PrepareSaveResult.Overwritten -> {
                workingFile = result.workingFile
                pendingEditedFile = null
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        fields = result.fields,
                        originalFields = result.fields,
                        customFields = result.customFields,
                        originalCustomFields = result.customFields,
                        tagGroups = result.groups,
                        snackbarMessage = "已写回原来的那张图。备份是 ${result.backupName}。所选位置没有另存。",
                    )
                }
            }

            is PrepareSaveResult.SavedAs -> {
                workingFile = result.workingFile
                pendingEditedFile = null
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        fields = result.fields,
                        originalFields = result.fields,
                        customFields = result.customFields,
                        originalCustomFields = result.customFields,
                        tagGroups = result.groups,
                        saveAsPrompt = null,
                        snackbarMessage = "已保存到 ${result.savedPath.ifBlank { "所选位置" }}，文件名是 ${result.newName}。原图备份是 ${result.backupName}。",
                    )
                }
            }

            is PrepareSaveResult.NeedWritePermission -> {
                pendingEditedFile = result.editedFile
                pendingDestination = result.destination
                pendingBackupName = result.backupName
                _uiState.update {
                    it.copy(
                        isSaving = true,
                        writeRequest = result.request,
                    )
                }
            }

            is PrepareSaveResult.NeedSaveAs -> {
                pendingEditedFile = result.editedFile
                pendingBackupName = result.backupName
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveAsPrompt = SaveAsPrompt(result.reason, result.backupName),
                    )
                }
            }

            is PrepareSaveResult.ValidationError -> {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.errors.joinToString("\n"),
                        snackbarMessage = result.errors.first(),
                    )
                }
            }

            is PrepareSaveResult.Error -> {
                _uiState.update {
                    it.copy(isSaving = false, snackbarMessage = result.message)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        workingFile?.delete()
        pendingEditedFile?.delete()
        val preview = _uiState.value.preview
        if (preview != null && !preview.isRecycled) preview.recycle()
    }

    private fun persistUriAccess(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        runCatching {
            resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    private fun recycleLater(previous: Bitmap?, keep: Bitmap?) {
        if (previous == null || previous === keep || previous.isRecycled) return
        viewModelScope.launch {
            delay(800)
            runCatching { if (!previous.isRecycled) previous.recycle() }
        }
    }
}

private fun List<CustomField>.customValueMap(): Map<String, String> {
    return associate { it.tag to it.value.trim() }
}
