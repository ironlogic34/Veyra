package com.ironlogic.veyra.data.video

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the device video library without copying or uploading media. */
class VideoLibraryRepository(
    private val contentResolver: ContentResolver,
) {
    suspend fun scan(): List<VideoItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
        )

        val collectionUri = if (
            VideoMediaStoreVolumePolicy.usesAggregateExternalVolume(Build.VERSION.SDK_INT)
        ) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val items = mutableListOf<VideoItem>()
        val cursor = contentResolver.query(
            collectionUri,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        ) ?: throw IllegalStateException("MediaStore returned no video cursor")

        cursor.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(collectionUri, id).toString()
                items += VideoItem(
                    id = contentUri,
                    contentUri = contentUri,
                    displayName = cursor.getString(nameColumn),
                    durationMs = cursor.getLong(durationColumn),
                    sizeBytes = cursor.getLong(sizeColumn),
                    addedEpochSeconds = cursor.getLong(addedColumn),
                    modifiedEpochSeconds = cursor.getLong(modifiedColumn),
                )
            }
        }
        items
    }
}

data class VideoItem(
    val id: String,
    val contentUri: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val addedEpochSeconds: Long,
    val modifiedEpochSeconds: Long = 0L,
    val sourceFolderUris: Set<String> = emptySet(),
)
