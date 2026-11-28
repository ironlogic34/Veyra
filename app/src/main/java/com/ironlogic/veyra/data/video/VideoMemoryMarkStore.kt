package com.ironlogic.veyra.data.video

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

data class VideoMemoryMark(
    val id: String,
    val contentUri: String,
    val displayName: String,
    val positionMs: Long,
    val note: String,
    val mood: String?,
    val createdAtEpochMs: Long,
)

object VideoMemoryMarkCodec {
    private const val FIELD_SEPARATOR = "\t"

    fun encode(marks: List<VideoMemoryMark>): String = marks.joinToString("\n") { mark ->
        listOf(
            encodeText(mark.id),
            encodeText(mark.contentUri),
            encodeText(mark.displayName),
            mark.positionMs.toString(),
            encodeText(mark.note),
            encodeText(mark.mood.orEmpty()),
            mark.createdAtEpochMs.toString(),
        ).joinToString(FIELD_SEPARATOR)
    }

    fun decode(encoded: String?): List<VideoMemoryMark> = encoded
        .orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val fields = line.split(FIELD_SEPARATOR)
            if (fields.size != 7) return@mapNotNull null
            runCatching {
                VideoMemoryMark(
                    id = decodeText(fields[0]),
                    contentUri = decodeText(fields[1]),
                    displayName = decodeText(fields[2]),
                    positionMs = fields[3].toLong(),
                    note = decodeText(fields[4]),
                    mood = decodeText(fields[5]).takeIf { it.isNotBlank() },
                    createdAtEpochMs = fields[6].toLong(),
                ).takeIf {
                    it.id.isNotBlank() &&
                        it.contentUri.isNotBlank() &&
                        it.positionMs >= 0L &&
                        it.createdAtEpochMs >= 0L
                }
            }.getOrNull()
        }
        .toList()

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}

class VideoMemoryMarkStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun readAll(): List<VideoMemoryMark> =
        VideoMemoryMarkCodec.decode(preferences.getString(KEY_MARKS, null))

    fun readForVideo(contentUri: String): List<VideoMemoryMark> =
        readAll().filter { it.contentUri == contentUri }

    fun add(
        video: VideoItem,
        positionMs: Long,
        note: String = "",
        mood: String? = null,
    ): VideoMemoryMark {
        val mark = VideoMemoryMark(
            id = UUID.randomUUID().toString(),
            contentUri = video.contentUri,
            displayName = video.displayName,
            positionMs = positionMs.coerceAtLeast(0L),
            note = note.trim(),
            mood = mood?.trim()?.takeIf { it.isNotEmpty() },
            createdAtEpochMs = System.currentTimeMillis(),
        )
        val updated = listOf(mark) + readAll()
        preferences.edit().putString(KEY_MARKS, VideoMemoryMarkCodec.encode(updated)).apply()
        return mark
    }

    fun remove(id: String) {
        val updated = readAll().filterNot { it.id == id }
        preferences.edit().putString(KEY_MARKS, VideoMemoryMarkCodec.encode(updated)).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_MARKS).apply()
    }

    private companion object {
        const val PREFERENCES = "veyra_memory_marks"
        const val KEY_MARKS = "marks"
    }
}

fun VideoMemoryMark.asVideoItem(): VideoItem = VideoItem(
    id = contentUri,
    contentUri = contentUri,
    displayName = displayName,
    durationMs = 0L,
    sizeBytes = 0L,
    addedEpochSeconds = 0L,
)
