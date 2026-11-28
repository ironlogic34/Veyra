package com.ironlogic.veyra.data.video

import android.content.Context
import java.security.MessageDigest

data class VideoSubtitleSelection(
    val uri: String,
    val displayName: String,
)

/** Stores the user's local subtitle selection separately for each video. */
class VideoSubtitleStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(videoUri: String): VideoSubtitleSelection? {
        val uri = preferences.getString(key(videoUri, URI_SUFFIX), null) ?: return null
        val name = preferences.getString(key(videoUri, NAME_SUFFIX), null) ?: "Subtitles"
        return VideoSubtitleSelection(uri, name)
    }

    fun write(videoUri: String, selection: VideoSubtitleSelection) {
        preferences.edit()
            .putString(key(videoUri, URI_SUFFIX), selection.uri)
            .putString(key(videoUri, NAME_SUFFIX), selection.displayName)
            .apply()
    }

    fun clear(videoUri: String) {
        preferences.edit()
            .remove(key(videoUri, URI_SUFFIX))
            .remove(key(videoUri, NAME_SUFFIX))
            .apply()
    }

    private fun key(videoUri: String, suffix: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(videoUri.toByteArray(Charsets.UTF_8))
        val stableId = digest.joinToString("") { byte -> "%02x".format(byte) }
        return "${suffix}_$stableId"
    }

    private companion object {
        const val PREFERENCES = "veyra_video_subtitles"
        const val URI_SUFFIX = "subtitle_uri"
        const val NAME_SUFFIX = "subtitle_name"
    }
}
