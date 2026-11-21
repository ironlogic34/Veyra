package com.ironlogic.veyra.data.video

import android.content.Context
import android.net.Uri

/** Persists the tree URIs the user explicitly selected through the Storage Access Framework. */
class VideoFolderStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(): List<String> =
        preferences.getStringSet(KEY_FOLDER_URIS, emptySet()).orEmpty().toList().sorted()

    fun add(uri: Uri) {
        val updated = preferences.getStringSet(KEY_FOLDER_URIS, emptySet()).orEmpty().toMutableSet()
        updated += uri.toString()
        preferences.edit().putStringSet(KEY_FOLDER_URIS, updated).apply()
    }

    fun remove(uri: Uri) {
        val updated = preferences.getStringSet(KEY_FOLDER_URIS, emptySet()).orEmpty().toMutableSet()
        updated -= uri.toString()
        preferences.edit().putStringSet(KEY_FOLDER_URIS, updated).apply()
    }

    private companion object {
        const val PREFERENCES = "veyra_video_folders"
        const val KEY_FOLDER_URIS = "selected_tree_uris"
    }
}
