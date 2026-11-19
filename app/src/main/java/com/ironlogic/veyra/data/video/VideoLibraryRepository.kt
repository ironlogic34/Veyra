package com.ironlogic.veyra.data.video

import android.content.ContentResolver
import android.net.Uri
import com.ironlogic.veyra.permissions.VideoAccessLevel
import kotlinx.coroutines.CancellationException

data class VideoLibraryResult(
    val videos: List<VideoItem>,
    val errors: List<String>
)

/** Keeps ordinary provider failures recoverable without turning cancellation into an empty result. */
internal suspend fun <T : Any> loadVideoSourceSafely(
    onFailure: (Exception) -> Unit,
    source: suspend () -> T
): T? = try {
    source()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    onFailure(failure)
    null
}

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
            loadVideoSourceSafely(
                onFailure = { failure ->
                    errors += if (failure is SecurityException) {
                        "Device-library permission changed."
                    } else {
                        "Could not read the device video library."
                    }
                }
            ) {
                MediaStoreVideoDataSource(contentResolver).getVideos()
            }?.let(sources::add)
        }

        folderUris.distinctBy(Uri::toString).forEachIndexed { index, folderUri ->
            loadVideoSourceSafely(
                onFailure = { failure ->
                    errors += if (failure is SecurityException) {
                        "Access to saved folder ${index + 1} was revoked."
                    } else {
                        "Saved folder ${index + 1} could not be scanned."
                    }
                }
            ) {
                SafVideoFolderDataSource(contentResolver).getVideos(folderUri)
            }?.let(sources::add)
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
