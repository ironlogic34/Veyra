package com.ironlogic.veyra.data.video

import java.util.Locale

/**
 * Merges ordered media sources while retaining the first occurrence of an identity.
 * The caller chooses the identity strategy; URI identity avoids guessing from filenames.
 */
internal object VideoLibraryMerger {
    fun <T> merge(
        sources: List<List<T>>,
        identity: (T) -> String,
        modifiedSeconds: (T) -> Long,
        displayName: (T) -> String
    ): List<T> {
        val unique = LinkedHashMap<String, T>()
        sources.forEach { source ->
            source.forEach { item ->
                unique.putIfAbsent(identity(item), item)
            }
        }
        return unique.values.sortedWith(
            compareByDescending<T> { modifiedSeconds(it) }
                .thenBy { displayName(it).lowercase(Locale.ROOT) }
        )
    }
}
