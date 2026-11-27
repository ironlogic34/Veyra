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
    suspend fun scan(
        folderUris: List<String>,
        previousVideos: List<VideoItem> = emptyList(),
        previousDirectories: List<VideoFolderDirectory> = emptyList(),
    ): VideoFolderScanResult = withContext(Dispatchers.IO) {
        val selectedRootUris = folderUris.distinct()
        val previousSnapshot = VideoFolderScanPolicy.retainSelectedRoots(
            existingVideos = previousVideos,
            existingDirectories = previousDirectories,
            selectedRootUris = selectedRootUris,
        )
        val folders = mutableListOf<VideoFolder>()
        val videos = previousSnapshot.videos.associateBy { it.contentUri }.toMutableMap()
        val directories = previousSnapshot.directories.toMutableList()
        val errors = mutableListOf<String>()

        for (rawUri in selectedRootUris) {
            currentCoroutineContext().ensureActive()
            val uri = Uri.parse(rawUri)
            var rootOpenFailure: Exception? = null
            val root = try {
                DocumentFile.fromTreeUri(context, uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                rootOpenFailure = exception
                null
            }
            val name = runCatching { root?.name }
                .getOrNull()
                ?.let { it.takeIf(String::isNotBlank) }
                ?: fallbackName(uri)
            val rootVideos = linkedMapOf<String, VideoItem>()
            val rootDirectories = mutableListOf<VideoFolderDirectory>()
            var accessible = root != null
            var outcome = VideoFolderScanOutcome.INCOMPLETE

            if (root == null) {
                outcome = rootOpenFailure?.let(VideoFolderScanPolicy::outcomeForRootOpenFailure)
                    ?: VideoFolderScanOutcome.ACCESS_LOST
                accessible = outcome != VideoFolderScanOutcome.ACCESS_LOST
                if (outcome == VideoFolderScanOutcome.ACCESS_LOST) {
                    errors += "Access to \"$name\" is unavailable. Select the folder again to restore access."
                } else {
                    errors += "Veyra couldn't inspect \"$name\". Its previous videos are being kept."
                }
            } else {
                val canRead = try {
                    root.canRead()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: SecurityException) {
                    false
                } catch (_: Exception) {
                    null
                }

                when (canRead) {
                    false -> {
                        accessible = false
                        outcome = VideoFolderScanOutcome.ACCESS_LOST
                        errors += "Access to \"$name\" is unavailable. Select the folder again to restore access."
                    }
                    null -> {
                        accessible = true
                        outcome = VideoFolderScanOutcome.INCOMPLETE
                        errors += "Veyra couldn't inspect \"$name\". Its previous videos are being kept."
                    }
                    true -> {
                        try {
                            collectVideos(root, rawUri, rootVideos, rootDirectories)
                            outcome = VideoFolderScanOutcome.COMPLETE
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: SecurityException) {
                            accessible = false
                            outcome = VideoFolderScanOutcome.ACCESS_LOST
                            errors += "Access to \"$name\" changed while scanning. Select it again to restore access."
                        } catch (_: Exception) {
                            accessible = true
                            outcome = VideoFolderScanOutcome.INCOMPLETE
                            errors += "Some videos in \"$name\" could not be read. Its previous videos are being kept."
                        }
                    }
                }
            }

            val committed = VideoFolderScanPolicy.commitRoot(
                existingVideos = videos.values.toList(),
                existingDirectories = directories,
                rootVideos = rootVideos.values.toList(),
                rootDirectories = rootDirectories,
                rootUri = rawUri,
                outcome = outcome,
            )
            videos.clear()
            committed.videos.forEach { item -> videos[item.contentUri] = item }
            directories.clear()
            directories.addAll(committed.directories)
            folders += VideoFolder(rawUri, name, accessible)
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
            VideoFolderMetadataPolicy.readDuration {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?: 0L
            }
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
