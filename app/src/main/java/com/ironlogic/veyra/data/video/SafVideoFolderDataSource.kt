package com.ironlogic.veyra.data.video

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Recursively reads videos from a user-selected SAF tree with a persisted read grant. */
class SafVideoFolderDataSource(private val contentResolver: ContentResolver) {
    suspend fun getVideos(treeUri: Uri): List<VideoItem> = withContext(Dispatchers.IO) {
        val pending = ArrayDeque<Pair<String, String>>()
        val visited = mutableSetOf<String>()
        val videos = mutableListOf<VideoItem>()
        pending.addLast(DocumentsContract.getTreeDocumentId(treeUri) to "")

        while (pending.isNotEmpty()) {
            val (parentId, parentPath) = pending.removeFirst()
            if (!visited.add(parentId)) continue

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idColumn) ?: continue
                    val name = cursor.getString(nameColumn) ?: "Untitled"
                    val mimeType = cursor.getString(mimeColumn)
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val path = if (parentPath.isBlank()) name else "${parentPath}/${name}"
                        pending.addLast(documentId to path)
                    } else if (mimeType?.startsWith("video/") == true) {
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        val size = if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn) else 0L
                        val modifiedMillis = if (modifiedColumn >= 0 && !cursor.isNull(modifiedColumn)) cursor.getLong(modifiedColumn) else 0L
                        videos += VideoItem(
                            contentUri = documentUri,
                            displayName = name,
                            durationMs = 0L,
                            sizeBytes = size,
                            dateModifiedSeconds = modifiedMillis / 1000L,
                            mimeType = mimeType,
                            relativePath = parentPath.ifBlank { null }
                        )
                    }
                }
            }
        }

        videos.sortedWith(
            compareByDescending<VideoItem> { it.dateModifiedSeconds }
                .thenBy { it.displayName.lowercase() }
        )
    }
}
