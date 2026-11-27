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

data class VideoFolderDirectory(
    val uri: String,
    val parentUri: String,
    val rootUri: String,
    val name: String,
)

data class VideoFolderScanResult(
    val folders: List<VideoFolder>,
    val videos: List<VideoItem>,
    val errors: List<String>,
    val directories: List<VideoFolderDirectory> = emptyList(),
)

/** Scans only folders the user explicitly granted through ACTION_OPEN_DOCUMENT_TREE. */
class VideoFolderRepository(
    private val context: Context,
) {
    suspend fun scan(folderUris: List<String>): VideoFolderScanResult = withContext(Dispatchers.IO) {
        val folders = mutableListOf<VideoFolder>()
        val videos = linkedMapOf<String, VideoItem>()
        val directories = mutableListOf<VideoFolderDirectory>()
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
                val rootVideos = linkedMapOf<String, VideoItem>()
                val rootDirectories = mutableListOf<VideoFolderDirectory>()
                var accessible = true
                try {
                    collectVideos(root, rawUri, rootVideos, rootDirectories)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: SecurityException) {
                    accessible = false
                    errors += "Access to \"$name\" changed while scanning. Select it again to restore access."
                } catch (_: Exception) {
                    errors += "Some videos in \"$name\" could not be read."
                }

                val committed = VideoFolderScanPolicy.commitRoot(
                    existingVideos = videos.values.toList(),
                    existingDirectories = directories,
                    rootVideos = rootVideos.values.toList(),
                    rootDirectories = rootDirectories,
                    rootAccessible = accessible,
                )
                videos.clear()
                committed.videos.forEach { item -> videos[item.contentUri] = item }
                directories.clear()
                directories.addAll(committed.directories)
                folders += VideoFolder(rawUri, name, accessible)
            }
        }

        VideoFolderScanResult(
            folders = folders,
            videos = videos.values.toList(),
            errors = errors,
            directories = directories,
        )
    }

    private suspend fun collectVideos(
        root: DocumentFile,
        folderUri: String,
        videos: MutableMap<String, VideoItem>,
        directories: MutableList<VideoFolderDirectory>,
    ) {
        data class PendingDirectory(
            val document: DocumentFile,
            val navigationUri: String,
            val ancestorUris: Set<String>,
        )

        val pending = ArrayDeque<PendingDirectory>()
        val visited = mutableSetOf<String>()
        pending.add(PendingDirectory(root, folderUri, setOf(folderUri)))

        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val directory = pending.removeFirst()
            if (!visited.add(directory.document.uri.toString())) continue

            for (child in directory.document.listFiles()) {
                currentCoroutineContext().ensureActive()
                when {
                    child.isDirectory -> {
                        val childUri = child.uri.toString()
                        directories += VideoFolderDirectory(
                            uri = childUri,
                            parentUri = directory.navigationUri,
                            rootUri = folderUri,
                            name = child.name?.takeIf { it.isNotBlank() } ?: "Untitled folder",
                        )
                        pending.add(
                            PendingDirectory(
                                document = child,
                                navigationUri = childUri,
                                ancestorUris = directory.ancestorUris + childUri,
                            ),
                        )
                    }
                    child.isFile && VideoFolderMediaTypePolicy.isVideo(child.type, child.name) -> {
                        val uri = child.uri
                        val uriString = uri.toString()
                        val existing = videos[uriString]
                        if (existing != null) {
                            videos[uriString] = existing.copy(
                                sourceFolderUris = existing.sourceFolderUris + folderUri,
                                sourceDirectoryUris = existing.sourceDirectoryUris + directory.ancestorUris,
                            )
                        } else {
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
                                sourceFolderUris = setOf(folderUri),
                                sourceDirectoryUris = directory.ancestorUris,
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
