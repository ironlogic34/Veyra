package com.ironlogic.veyra.data.video

import java.util.Locale

/**
 * Selects deterministic "related video" candidates without coupling the ranking
 * to Android UI or media-provider APIs.
 *
 * Videos from the same folder rank first, followed by exact MIME-type matches.
 * Remaining library items are retained as fallback results, newest first.
 */
internal object VideoRelatedItems {
    fun <T> select(
        currentMediaUri: String,
        currentRelativePath: String?,
        currentMimeType: String?,
        candidates: List<T>,
        limit: Int = DEFAULT_LIMIT,
        mediaUri: (T) -> String,
        relativePath: (T) -> String?,
        mimeType: (T) -> String?,
        modifiedSeconds: (T) -> Long,
        displayName: (T) -> String
    ): List<T> {
        require(currentMediaUri.isNotBlank()) { "currentMediaUri must not be blank." }
        require(limit >= 0) { "limit must not be negative." }
        if (limit == 0) return emptyList()

        val currentFolder = normalizeFolder(currentRelativePath)
        val currentMime = normalizeMimeType(currentMimeType)

        return candidates
            .asSequence()
            .filter { mediaUri(it) != currentMediaUri }
            .distinctBy(mediaUri)
            .sortedWith(
                compareByDescending<T> { candidate ->
                    relevanceScore(
                        currentFolder = currentFolder,
                        currentMime = currentMime,
                        candidateFolder = normalizeFolder(relativePath(candidate)),
                        candidateMime = normalizeMimeType(mimeType(candidate))
                    )
                }
                    .thenByDescending(modifiedSeconds)
                    .thenBy { displayName(it).lowercase(Locale.ROOT) }
                    .thenBy(mediaUri)
            )
            .take(limit)
            .toList()
    }

    private fun relevanceScore(
        currentFolder: String?,
        currentMime: String?,
        candidateFolder: String?,
        candidateMime: String?
    ): Int {
        val sameFolder = currentFolder != null && currentFolder == candidateFolder
        val sameMimeType = currentMime != null && currentMime == candidateMime

        return (if (sameFolder) SAME_FOLDER_SCORE else 0) +
            (if (sameMimeType) SAME_MIME_TYPE_SCORE else 0)
    }

    private fun normalizeFolder(path: String?): String? =
        path
            ?.trim()
            ?.replace('\\', '/')
            ?.trim('/')
            ?.lowercase(Locale.ROOT)
            ?.takeIf(String::isNotEmpty)

    private fun normalizeMimeType(mimeType: String?): String? =
        mimeType?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty)

    private const val SAME_FOLDER_SCORE = 2
    private const val SAME_MIME_TYPE_SCORE = 1
    private const val DEFAULT_LIMIT = 10
}
