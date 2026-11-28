package com.ironlogic.veyra.data.video

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Reads the device video library without copying or uploading media. */
class VideoLibraryRepository(
    private val contentResolver: ContentResolver,
) {
    suspend fun scan(): List<VideoItem> = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.DATE_ADDED)
            add(MediaStore.Video.Media.DATE_MODIFIED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            }
        }.toTypedArray()

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
            currentCoroutineContext().ensureActive()
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val relativePathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(collectionUri, id).toString()
                items += VideoItem(
                    id = contentUri,
                    contentUri = contentUri,
                    displayName = VideoMediaStoreMetadataPolicy.displayName(cursor.getString(nameColumn)),
                    durationMs = VideoMediaStoreMetadataPolicy.nonNegative(cursor.getLong(durationColumn)),
                    sizeBytes = VideoMediaStoreMetadataPolicy.nonNegative(cursor.getLong(sizeColumn)),
                    addedEpochSeconds = VideoMediaStoreMetadataPolicy.nonNegative(cursor.getLong(addedColumn)),
                    modifiedEpochSeconds = VideoMediaStoreMetadataPolicy.nonNegative(cursor.getLong(modifiedColumn)),
                    relativePath = if (relativePathColumn >= 0) {
                        cursor.getString(relativePathColumn)
                            ?.replace('\\', '/')
                            ?.trim('/')
                            ?.takeIf { it.isNotBlank() }
                    } else {
                        null
                    },
                )
            }
        }
        currentCoroutineContext().ensureActive()
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
    val sourceDirectoryUris: Set<String> = emptySet(),
    val relativePath: String? = null,
)
