package com.warabi1062.burehan

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Locale

data class PhotoEntry(val uri: Uri, val dateTaken: Long)

class PhotoRepository(private val contentResolver: ContentResolver) {

    private val imageMimeTypes = setOf("image/jpeg", "image/png", "image/webp", "image/heif", "image/heic")
    private val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    fun loadPhotoEntriesFromTree(treeUri: Uri): List<PhotoEntry> {
        val entries = mutableListOf<PhotoEntry>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        collectImages(childrenUri, treeUri, entries)
        return entries.sortedByDescending { it.dateTaken }
    }

    private fun collectImages(childrenUri: Uri, treeUri: Uri, result: MutableList<PhotoEntry>) {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val docId = cursor.getString(idColumn)
                val mimeType = cursor.getString(mimeColumn)
                val lastModified = cursor.getLong(modifiedColumn)
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val subChildren = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                    collectImages(subChildren, treeUri, result)
                } else if (mimeType in imageMimeTypes) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    val dateTaken = readDateTaken(docUri) ?: lastModified
                    result.add(PhotoEntry(docUri, dateTaken))
                }
            }
        }
    }

    private fun readDateTaken(uri: Uri): Long? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                dateStr?.let { exifDateFormat.parse(it)?.time }
            }
        } catch (_: Exception) {
            null
        }
    }
}
