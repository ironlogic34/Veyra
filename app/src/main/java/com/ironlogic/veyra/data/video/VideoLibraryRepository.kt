package com.ironlogic.veyra.data.video

import android.content.ContentResolver
import android.net.Uri
import com.ironlogic.veyra.permissions.VideoAccessLevel

data class VideoLibraryResult(
    val videos: List<VideoItem>,
    val errors: List<String>
)

/**
 * Combines device-library and user-selected-folder sources.
 *
 * A failure in one source does not discard results from another source.
 * Exact content-URI identity is used for deduplication; filenames are not identities.
 */
class VideoLibraryRepository(
    private val contentResolver: ContentResolver
) {
    suspend fun load(
        accessLevel: VideoAccessLevel,
        folderUris: List<Uri>
    ): VideoLibraryResult {
        val sources = mutableListOf<List<VideoItem>>()
        val errors = mutableListOf<String>()

        if (accessLevel != VideoAccessLevel.NONE) {
            try {
                sources += MediaStoreVideoDataSource(contentResolver).getVideos()
            } catch (_: SecurityException) {
                errors += "Device-library permission changed."
            } catch (_: Exception) {
                errors += "Could not read the device video library."
            }
        }

        folderUris.distinctBy(Uri::toString).forEachIndexed { index, folderUri ->
            try {
                sources += SafVideoFolderDataSource(contentResolver).getVideos(folderUri)
            } catch (_: SecurityException) {
                errors += "Access to saved folder ${index + 1} was revoked."
            } catch (_: Exception) {
                errors += "Saved folder ${index + 1} could not be scanned."
            }
        }

        val merged = VideoLibraryMerger.merge(
            sources = sources,
            identity = { it.contentUri.toString() },
            modifiedSeconds = { it.dateModifiedSeconds },
            displayName = { it.displayName }
        )
        return VideoLibraryResult(videos = merged, errors = errors.distinct())
    }
}
