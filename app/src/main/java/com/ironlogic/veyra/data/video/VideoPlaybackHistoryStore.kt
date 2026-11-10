package com.ironlogic.veyra.data.video

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Durable local playback history backed by SQLite.
 *
 * All database work runs on Dispatchers.IO. Media content URIs are the primary
 * key; filenames and paths are deliberately not used as identities.
 */
class VideoPlaybackHistoryStore(context: Context) : AutoCloseable {
    private val database = HistoryDatabase(context.applicationContext)

    suspend fun save(record: VideoPlaybackRecord) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COLUMN_MEDIA_URI, record.mediaUri)
            put(COLUMN_POSITION_MS, record.positionMs)
            if (record.durationMs == null) putNull(COLUMN_DURATION_MS)
            else put(COLUMN_DURATION_MS, record.durationMs)
            put(COLUMN_LAST_PLAYED_AT, record.lastPlayedAtEpochMs)
            put(COLUMN_COMPLETED, if (record.isCompleted) 1 else 0)
        }

        val rowId = database.writableDatabase.insertWithOnConflict(
            TABLE_HISTORY,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        check(rowId != -1L) { "Could not save video playback history." }
    }

    suspend fun find(mediaUri: String): VideoPlaybackRecord? = withContext(Dispatchers.IO) {
        require(mediaUri.isNotBlank()) { "mediaUri must not be blank." }

        database.readableDatabase.query(
            TABLE_HISTORY,
            HISTORY_COLUMNS,
            "$COLUMN_MEDIA_URI = ?",
            arrayOf(mediaUri),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toPlaybackRecord() else null
        }
    }

    suspend fun recent(limit: Int = DEFAULT_RECENT_LIMIT): List<VideoPlaybackRecord> =
        withContext(Dispatchers.IO) {
            require(limit > 0) { "limit must be positive." }

            database.readableDatabase.query(
                TABLE_HISTORY,
                HISTORY_COLUMNS,
                null,
                null,
                null,
                null,
                "$COLUMN_LAST_PLAYED_AT DESC, $COLUMN_MEDIA_URI ASC",
                limit.toString()
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.toPlaybackRecord())
                }
            }
        }

    suspend fun delete(mediaUri: String): Boolean = withContext(Dispatchers.IO) {
        require(mediaUri.isNotBlank()) { "mediaUri must not be blank." }
        database.writableDatabase.delete(
            TABLE_HISTORY,
            "$COLUMN_MEDIA_URI = ?",
            arrayOf(mediaUri)
        ) > 0
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        database.writableDatabase.delete(TABLE_HISTORY, null, null)
    }

    override fun close() {
        database.close()
    }

    private class HistoryDatabase(context: Context) : SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_HISTORY (
                    $COLUMN_MEDIA_URI TEXT NOT NULL PRIMARY KEY,
                    $COLUMN_POSITION_MS INTEGER NOT NULL CHECK ($COLUMN_POSITION_MS >= 0),
                    $COLUMN_DURATION_MS INTEGER CHECK (
                        $COLUMN_DURATION_MS IS NULL OR $COLUMN_DURATION_MS >= 0
                    ),
                    $COLUMN_LAST_PLAYED_AT INTEGER NOT NULL CHECK (
                        $COLUMN_LAST_PLAYED_AT >= 0
                    ),
                    $COLUMN_COMPLETED INTEGER NOT NULL DEFAULT 0 CHECK (
                        $COLUMN_COMPLETED IN (0, 1)
                    )
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX index_video_history_last_played " +
                    "ON $TABLE_HISTORY ($COLUMN_LAST_PLAYED_AT DESC)"
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Never drop user history. Add explicit, sequential migrations before
            // increasing DATABASE_VERSION in a future release.
            error("Missing video-history migration from version $oldVersion to $newVersion.")
        }
    }

    private companion object {
        const val DATABASE_NAME = "veyra_video_history.db"
        const val DATABASE_VERSION = 1
        const val TABLE_HISTORY = "video_playback_history"
        const val COLUMN_MEDIA_URI = "media_uri"
        const val COLUMN_POSITION_MS = "position_ms"
        const val COLUMN_DURATION_MS = "duration_ms"
        const val COLUMN_LAST_PLAYED_AT = "last_played_at"
        const val COLUMN_COMPLETED = "is_completed"
        const val DEFAULT_RECENT_LIMIT = 50

        val HISTORY_COLUMNS = arrayOf(
            COLUMN_MEDIA_URI,
            COLUMN_POSITION_MS,
            COLUMN_DURATION_MS,
            COLUMN_LAST_PLAYED_AT,
            COLUMN_COMPLETED
        )

        fun Cursor.toPlaybackRecord(): VideoPlaybackRecord {
            val durationIndex = getColumnIndexOrThrow(COLUMN_DURATION_MS)
            return VideoPlaybackRecord(
                mediaUri = getString(getColumnIndexOrThrow(COLUMN_MEDIA_URI)),
                positionMs = getLong(getColumnIndexOrThrow(COLUMN_POSITION_MS)),
                durationMs = if (isNull(durationIndex)) null else getLong(durationIndex),
                lastPlayedAtEpochMs = getLong(
                    getColumnIndexOrThrow(COLUMN_LAST_PLAYED_AT)
                ),
                isCompleted = getInt(getColumnIndexOrThrow(COLUMN_COMPLETED)) != 0
            )
        }
    }
}
