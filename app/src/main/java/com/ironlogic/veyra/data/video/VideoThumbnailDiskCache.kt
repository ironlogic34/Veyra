package com.ironlogic.veyra.data.video

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/** A bounded, local-only cache for already-generated thumbnail bytes. */
class VideoThumbnailDiskCache(
    private val directory: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) {
    private val lock = Any()

    init {
        require(maxBytes > 0L) { "maxBytes must be positive" }
    }

    fun read(key: String): ByteArray? = synchronized(lock) {
        val file = fileFor(key)
        if (!runCatching { file.isFile }.getOrDefault(false)) return@synchronized null

        val bytes = runCatching {
            file.readBytes().also { file.setLastModified(System.currentTimeMillis()) }
        }.getOrNull()

        if (bytes == null || bytes.isEmpty()) {
            runCatching { file.delete() }
            null
        } else {
            bytes
        }
    }

    fun write(key: String, bytes: ByteArray) = synchronized(lock) {
        if (bytes.isEmpty() || bytes.size.toLong() > maxBytes) return@synchronized

        val destination = fileFor(key)
        val temporary = File(directory, "${destination.name}.tmp")
        try {
            if (!directory.isDirectory && !directory.mkdirs()) return@synchronized
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            if (!temporary.renameTo(destination)) {
                destination.delete()
                if (!temporary.renameTo(destination)) return@synchronized
            }
            trimToSize()
        } catch (_: IOException) {
            // Thumbnail caching is optional; a storage failure must not break the library.
        } catch (_: SecurityException) {
            // Keep playback and library rendering available if cache access is denied.
        } finally {
            runCatching { temporary.delete() }
        }
    }

    fun remove(key: String) = synchronized(lock) {
        runCatching { fileFor(key).delete() }
        Unit
    }

    fun clear() = synchronized(lock) {
        runCatching { directory.listFiles()?.forEach(File::delete) }
        Unit
    }

    private fun trimToSize() {
        val files = directory.listFiles { file ->
            file.isFile && file.extension == CACHE_EXTENSION
        }?.toList().orEmpty()
        var totalBytes = files.sumOf(File::length)

        files.sortedBy(File::lastModified).forEach { file ->
            if (totalBytes <= maxBytes) return
            val fileSize = file.length()
            if (file.delete()) totalBytes -= fileSize
        }
    }

    private fun fileFor(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
        val name = digest.joinToString("") { byte -> "%02x".format(byte) }
        return File(directory, "$name.$CACHE_EXTENSION")
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 32L * 1024L * 1024L
        private const val CACHE_EXTENSION = "jpg"
    }
}

object VideoThumbnailCacheKey {
    fun forVideo(video: VideoItem): String =
        "${video.contentUri}\\u0000${video.sizeBytes}\\u0000${video.addedEpochSeconds}\\u0000${video.modifiedEpochSeconds}"
}
