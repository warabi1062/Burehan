package com.warabi1062.burehan

import android.app.Application
import android.net.Uri
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
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state

    private val repository = PhotoRepository(application.contentResolver)
    private val analyzer = SharpnessAnalyzer(application.contentResolver)

    fun startScan(treeUri: Uri) {
        if (_state.value.isScanning) return

        viewModelScope.launch {
            _state.value = ScanState(isScanning = true, folderSelected = true)

            val uris = withContext(Dispatchers.IO) { repository.loadPhotoUrisFromTree(treeUri) }
            _state.value = _state.value.copy(totalCount = uris.size)

            val results = mutableListOf<PhotoItem>()
            for ((index, uri) in uris.withIndex()) {
                val score = withContext(Dispatchers.IO) { analyzer.analyze(uri) }
                results.add(PhotoItem(uri, score))
                _state.value = _state.value.copy(
                    photos = results.toList(),
                    scannedCount = index + 1,
                )
            }

            _state.value = _state.value.copy(isScanning = false)
        }
    }
}
