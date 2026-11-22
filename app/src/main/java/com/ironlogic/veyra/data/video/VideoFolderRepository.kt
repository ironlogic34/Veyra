package com.ironlogic.veyra.data.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

data class VideoFolder(
    val uri: String,
    val name: String,
    val accessible: Boolean,
)

data class VideoFolderScanResult(
    val folders: List<VideoFolder>,
    val videos: List<VideoItem>,
    val errors: List<String>,
)

/** Scans only folders the user explicitly granted through ACTION_OPEN_DOCUMENT_TREE. */
class VideoFolderRepository(
    private val context: Context,
) {
    suspend fun scan(folderUris: List<String>): VideoFolderScanResult = withContext(Dispatchers.IO) {
        val folders = mutableListOf<VideoFolder>()
        val videos = linkedMapOf<String, VideoItem>()
        val errors = mutableListOf<String>()

        for (rawUri in folderUris.distinct()) {
            currentCoroutineContext().ensureActive()
            val uri = Uri.parse(rawUri)
            val root = runCatching { DocumentFile.fromTreeUri(context, uri) }.getOrNull()
            val name = root?.name?.takeIf { it.isNotBlank() } ?: fallbackName(uri)

            if (root == null || !root.canRead()) {
                folders += VideoFolder(rawUri, name, accessible = false)
                errors += "Access to \"$name\" is unavailable. Select the folder again to restore access."
            } else {
                var accessible = true
                try {
                    collectVideos(root, videos)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: SecurityException) {
                    accessible = false
                    errors += "Access to \"$name\" changed while scanning. Select it again to restore access."
                } catch (_: Exception) {
                    errors += "Some videos in \"$name\" could not be read."
                }
                folders += VideoFolder(rawUri, name, accessible)
            }
        }

        VideoFolderScanResult(folders, videos.values.toList(), errors)
    }

    private suspend fun collectVideos(
        root: DocumentFile,
        videos: MutableMap<String, VideoItem>,
    ) {
        val pending = ArrayDeque<DocumentFile>()
        val visited = mutableSetOf<String>()
        pending.add(root)

        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val directory = pending.removeFirst()
            if (!visited.add(directory.uri.toString())) continue

            for (child in directory.listFiles()) {
                when {
                    child.isDirectory -> pending.add(child)
                    child.isFile && VideoFolderMediaTypePolicy.isVideo(child.type, child.name) -> {
                        val uri = child.uri
                        val uriString = uri.toString()
                        if (uriString !in videos) {
                            val name = child.name?.takeIf { it.isNotBlank() } ?: "Untitled video"
                            val modifiedEpochSeconds = (child.lastModified() / 1_000L).coerceAtLeast(0L)
                            videos[uriString] = VideoItem(
                                id = uriString,
                                contentUri = uriString,
                                displayName = name,
                                durationMs = readDuration(uri),
                                sizeBytes = child.length().coerceAtLeast(0L),
                                addedEpochSeconds = modifiedEpochSeconds,
                                modifiedEpochSeconds = modifiedEpochSeconds,
                            )
                        }
                    }
                }
            }
            currentCoroutineContext().ensureActive()
        }
    }

    private fun readDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun fallbackName(uri: Uri): String =
        uri.lastPathSegment
            ?.substringAfter(':')
            ?.takeIf { it.isNotBlank() }
            ?: "Selected folder"
}
