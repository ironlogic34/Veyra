package com.ironlogic.veyra.data.video

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64

data class VideoHistoryEntry(
    val contentUri: String,
    val displayName: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedEpochMs: Long,
)

object VideoPlaybackHistoryCodec {
    private const val FIELD_SEPARATOR = "\t"

    fun encode(entries: List<VideoHistoryEntry>): String = entries.joinToString("\n") { entry ->
        listOf(
            encodeText(entry.contentUri),
            encodeText(entry.displayName),
            entry.positionMs.toString(),
            entry.durationMs.toString(),
            entry.lastPlayedEpochMs.toString(),
        ).joinToString(FIELD_SEPARATOR)
    }

    fun decode(encoded: String?): List<VideoHistoryEntry> = encoded
        .orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val fields = line.split(FIELD_SEPARATOR)
            if (fields.size != 5) return@mapNotNull null
            runCatching {
                VideoHistoryEntry(
                    contentUri = decodeText(fields[0]),
                    displayName = decodeText(fields[1]),
                    positionMs = fields[2].toLong(),
                    durationMs = fields[3].toLong(),
                    lastPlayedEpochMs = fields[4].toLong(),
                ).takeIf {
                    it.contentUri.isNotBlank() &&
                        it.positionMs >= 0L &&
                        it.durationMs >= 0L &&
                        it.lastPlayedEpochMs >= 0L &&
                        (it.durationMs == 0L || it.positionMs <= it.durationMs)
                }
            }.getOrNull()
        }
        .toList()

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}

class VideoPlaybackHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(): List<VideoHistoryEntry> =
        VideoPlaybackHistoryCodec.decode(preferences.getString(KEY_ENTRIES, null))

    fun record(video: VideoItem, positionMs: Long, durationMs: Long = video.durationMs) {
        val entry = VideoHistoryEntry(
            contentUri = video.contentUri,
            displayName = video.displayName,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L),
            lastPlayedEpochMs = System.currentTimeMillis(),
        )
        val updated = (listOf(entry) + read().filterNot { it.contentUri == video.contentUri })
            .take(MAX_ENTRIES)
        preferences.edit().putString(KEY_ENTRIES, VideoPlaybackHistoryCodec.encode(updated)).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_ENTRIES).apply()
    }

    private companion object {
        const val PREFERENCES = "veyra_playback_history"
        const val KEY_ENTRIES = "entries"
        const val MAX_ENTRIES = 50
    }
}

fun VideoHistoryEntry.asVideoItem(): VideoItem = VideoItem(
    id = contentUri,
    contentUri = contentUri,
    displayName = displayName,
    durationMs = durationMs,
    sizeBytes = 0L,
    addedEpochSeconds = 0L,
)
