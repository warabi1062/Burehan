package com.warabi1062.burehan

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScanState(
    val photos: List<PhotoItem> = emptyList(),
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val folderSelected: Boolean = false,
    val selectedUris: Set<Uri> = emptySet(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state

    private val contentResolver = application.contentResolver
    private val repository = PhotoRepository(contentResolver)
    private val analyzer = SharpnessAnalyzer(contentResolver)

    fun startScan(treeUri: Uri) {
        if (_state.value.isScanning) return

        viewModelScope.launch {
            _state.value = ScanState(isScanning = true, folderSelected = true)

            val entries = withContext(Dispatchers.IO) { repository.loadPhotoEntriesFromTree(treeUri) }
            _state.value = _state.value.copy(totalCount = entries.size)

            val results = mutableListOf<PhotoItem>()
            for ((index, entry) in entries.withIndex()) {
                val score = withContext(Dispatchers.IO) { analyzer.analyze(entry.uri) }
                results.add(PhotoItem(entry.uri, score, entry.dateTaken))
                _state.value = _state.value.copy(
                    photos = results.toList(),
                    scannedCount = index + 1,
                )
            }

            _state.value = _state.value.copy(isScanning = false)
        }
    }

    fun toggleSelection(uri: Uri) {
        val current = _state.value.selectedUris
        _state.value = _state.value.copy(
            selectedUris = if (uri in current) current - uri else current + uri,
        )
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedUris = emptySet())
    }

    fun deleteSelected() {
        val toDelete = _state.value.selectedUris
        if (toDelete.isEmpty()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                for (uri in toDelete) {
                    try {
                        DocumentsContract.deleteDocument(contentResolver, uri)
                    } catch (_: Exception) {
                    }
                }
            }
            _state.value = _state.value.copy(
                photos = _state.value.photos.filter { it.uri !in toDelete },
                selectedUris = emptySet(),
            )
        }
    }
}
