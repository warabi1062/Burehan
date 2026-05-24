package com.warabi1062.burehan

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

data class PhotoEntry(val uri: Uri, val displayName: String)

class PhotoRepository(private val contentResolver: ContentResolver) {

    private val imageMimeTypes = setOf("image/jpeg", "image/png", "image/webp", "image/heif", "image/heic")

    fun loadPhotoEntriesFromTree(treeUri: Uri): List<PhotoEntry> {
        val entries = mutableListOf<PhotoEntry>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        collectImages(childrenUri, treeUri, entries)
        return entries.sortedBy { it.displayName }
    }

    private fun collectImages(childrenUri: Uri, treeUri: Uri, result: MutableList<PhotoEntry>) {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val docId = cursor.getString(idColumn)
                val mimeType = cursor.getString(mimeColumn)
                val displayName = cursor.getString(nameColumn) ?: ""
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val subChildren = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                    collectImages(subChildren, treeUri, result)
                } else if (mimeType in imageMimeTypes) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    result.add(PhotoEntry(docUri, displayName))
                }
            }
        }
    }
}
