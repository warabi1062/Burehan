package com.warabi1062.burehan

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

class PhotoRepository(private val contentResolver: ContentResolver) {

    private val imageMimeTypes = setOf("image/jpeg", "image/png", "image/webp", "image/heif", "image/heic")

    fun loadPhotoUrisFromTree(treeUri: Uri): List<Uri> {
        val uris = mutableListOf<Uri>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        collectImages(childrenUri, treeUri, uris)
        return uris
    }

    private fun collectImages(childrenUri: Uri, treeUri: Uri, result: MutableList<Uri>) {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val docId = cursor.getString(idColumn)
                val mimeType = cursor.getString(mimeColumn)
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val subChildren = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                    collectImages(subChildren, treeUri, result)
                } else if (mimeType in imageMimeTypes) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    result.add(docUri)
                }
            }
        }
    }
}
