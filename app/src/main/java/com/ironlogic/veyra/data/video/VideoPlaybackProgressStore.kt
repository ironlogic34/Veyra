package com.ironlogic.veyra.data.video

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64

class VideoPlaybackProgressStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /**
     * Returns the most recently updated unfinished video, which drives Continue Watching.
     * Older per-video positions remain available through [read] with a video argument.
     */
    fun read(): VideoProgress? {
        val latestUri = preferences.getString(KEY_URI, null)
        if (latestUri != null) {
            val latest = readStored(latestUri) ?: readLegacy(latestUri)
            if (latest != null) {
                if (preferences.getString(progressKey(latestUri), null) == null) {
                    writeStored(latest)
                }
                writeLastSnapshot(latest)
                return latest
            }
            clearLastSnapshot()
        }

        val latest = readLatestStored() ?: return null
        writeLastSnapshot(latest)
        return latest
    }

    /** Returns this video's own saved position, not just the global Continue Watching item. */
    fun read(video: VideoItem): VideoProgress? {
        readStored(video.contentUri)?.let { return it }

        val legacy = readLegacy(video.contentUri) ?: return null
        writeStored(legacy)
        writeLastSnapshot(legacy)
        return legacy
    }

    fun save(video: VideoItem, positionMs: Long, durationMs: Long = video.durationMs) {
        val normalizedDurationMs = durationMs.coerceAtLeast(0L)
        if (!VideoResumePolicy.shouldResume(positionMs, normalizedDurationMs)) {
            clear(video)
            return
        }

        val progress = VideoProgress(
            contentUri = video.contentUri,
            displayName = video.displayName,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = normalizedDurationMs,
            lastUpdatedEpochMs = System.currentTimeMillis(),
        )
        preferences.edit()
            .putString(progressKey(video.contentUri), VideoPlaybackProgressCodec.encode(progress))
            .putString(KEY_URI, progress.contentUri)
            .putString(KEY_NAME, progress.displayName)
            .putLong(KEY_POSITION, progress.positionMs)
            .putLong(KEY_DURATION, progress.durationMs)
            .putLong(KEY_UPDATED_AT, progress.lastUpdatedEpochMs)
            .apply()
    }

    fun clear(video: VideoItem) {
        val wasLatest = preferences.getString(KEY_URI, null) == video.contentUri
        preferences.edit().remove(progressKey(video.contentUri)).apply()
        if (wasLatest) {
            val nextLatest = readLatestStored()
            if (nextLatest == null) {
                clearLastSnapshot()
            } else {
                writeLastSnapshot(nextLatest)
            }
        }
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun readStored(contentUri: String): VideoProgress? {
        val key = progressKey(contentUri)
        val encoded = preferences.getString(key, null) ?: return null
        val progress = VideoPlaybackProgressCodec.decode(encoded)
        if (
            progress == null ||
            progress.contentUri != contentUri ||
            !VideoResumePolicy.shouldResume(progress.positionMs, progress.durationMs)
        ) {
            preferences.edit().remove(key).apply()
            return null
        }
        return progress
    }

    /** Reads and migrates the single-position format written by earlier Veyra builds. */
    private fun readLegacy(contentUri: String): VideoProgress? {
        if (preferences.getString(KEY_URI, null) != contentUri) return null

        val positionMs = preferences.getLong(KEY_POSITION, 0L)
        val durationMs = preferences.getLong(KEY_DURATION, 0L)
        if (!VideoResumePolicy.shouldResume(positionMs, durationMs)) return null

        return VideoProgress(
            contentUri = contentUri,
            displayName = preferences.getString(KEY_NAME, "Untitled video") ?: "Untitled video",
            positionMs = positionMs,
            durationMs = durationMs,
            lastUpdatedEpochMs = preferences.getLong(KEY_UPDATED_AT, 0L)
                .takeIf { it > 0L }
                ?: System.currentTimeMillis(),
        )
    }

    private fun readLatestStored(): VideoProgress? {
        val entries = preferences.all.asSequence()
            .filter { (key, _) -> key.startsWith(KEY_VIDEO_PREFIX) }
            .mapNotNull { (_, value) ->
                (value as? String)?.let(VideoPlaybackProgressCodec::decode)
            }
        return VideoPlaybackProgressPolicy.mostRecent(entries.asIterable())
    }

    private fun writeStored(progress: VideoProgress) {
        preferences.edit()
            .putString(progressKey(progress.contentUri), VideoPlaybackProgressCodec.encode(progress))
            .apply()
    }

    private fun writeLastSnapshot(progress: VideoProgress) {
        preferences.edit()
            .putString(KEY_URI, progress.contentUri)
            .putString(KEY_NAME, progress.displayName)
            .putLong(KEY_POSITION, progress.positionMs)
            .putLong(KEY_DURATION, progress.durationMs)
            .putLong(KEY_UPDATED_AT, progress.lastUpdatedEpochMs)
            .apply()
    }

    private fun clearLastSnapshot() {
        preferences.edit()
            .remove(KEY_URI)
            .remove(KEY_NAME)
            .remove(KEY_POSITION)
            .remove(KEY_DURATION)
            .remove(KEY_UPDATED_AT)
            .apply()
    }

    private fun progressKey(contentUri: String): String =
        KEY_VIDEO_PREFIX + Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(contentUri.toByteArray(StandardCharsets.UTF_8))

    private companion object {
        const val PREFERENCES = "veyra_playback_progress"
        const val KEY_URI = "content_uri"
        const val KEY_NAME = "display_name"
        const val KEY_POSITION = "position_ms"
        const val KEY_DURATION = "duration_ms"
        const val KEY_UPDATED_AT = "updated_at_epoch_ms"
        const val KEY_VIDEO_PREFIX = "video_progress_"
    }
}

object VideoPlaybackProgressCodec {
    private const val FIELD_SEPARATOR = "\t"

    fun encode(progress: VideoProgress): String = listOf(
        encodeText(progress.contentUri),
        encodeText(progress.displayName),
        progress.positionMs.toString(),
        progress.durationMs.toString(),
        progress.lastUpdatedEpochMs.toString(),
    ).joinToString(FIELD_SEPARATOR)

    fun decode(encoded: String?): VideoProgress? {
        val fields = encoded?.split(FIELD_SEPARATOR) ?: return null
        if (fields.size != 5) return null

        return runCatching {
            VideoProgress(
                contentUri = decodeText(fields[0]),
                displayName = decodeText(fields[1]),
                positionMs = fields[2].toLong(),
                durationMs = fields[3].toLong(),
                lastUpdatedEpochMs = fields[4].toLong(),
            ).takeIf {
                it.contentUri.isNotBlank() &&
                    it.positionMs >= 0L &&
                    it.durationMs >= 0L &&
                    it.lastUpdatedEpochMs >= 0L &&
                    (it.durationMs == 0L || it.positionMs <= it.durationMs)
            }
        }.getOrNull()
    }

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}

object VideoPlaybackProgressPolicy {
    /** Picks the newest unfinished progress entry, with a stable tie-breaker. */
    fun mostRecent(progresses: Iterable<VideoProgress>): VideoProgress? =
        progresses.asSequence()
            .filter { progress ->
                progress.contentUri.isNotBlank() &&
                    progress.lastUpdatedEpochMs >= 0L &&
                    VideoResumePolicy.shouldResume(progress.positionMs, progress.durationMs)
            }
            .maxWithOrNull(
                compareBy<VideoProgress> { it.lastUpdatedEpochMs }
                    .thenBy { it.contentUri },
            )
}

data class VideoProgress(
    val contentUri: String,
    val displayName: String,
    val positionMs: Long,
    val durationMs: Long = 0L,
    val lastUpdatedEpochMs: Long = 0L,
)

fun VideoProgress.asVideoItem(): VideoItem = VideoItem(
    id = contentUri,
    contentUri = contentUri,
    displayName = displayName,
    durationMs = durationMs,
    sizeBytes = 0L,
    addedEpochSeconds = 0L,
)
