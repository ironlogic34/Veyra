package com.ironlogic.veyra.data.video

import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoThumbnailDiskCacheTest {
    @Test
    fun `writes and reads thumbnail bytes by key`() = withCache { cache, _ ->
        val bytes = byteArrayOf(1, 2, 3, 4)

        cache.write("content://video/1", bytes)

        assertArrayEquals(bytes, cache.read("content://video/1"))
    }

    @Test
    fun `evicts least recently used files when the cache exceeds its limit`() = withCache(maxBytes = 6L) { cache, directory ->
        cache.write("older", byteArrayOf(1, 2, 3, 4))
        directory.listFiles()!!.single().setLastModified(1L)

        val newest = byteArrayOf(5, 6, 7, 8)
        cache.write("newer", newest)

        assertNull(cache.read("older"))
        assertArrayEquals(newest, cache.read("newer"))
        assertEquals(4L, directory.listFiles()!!.sumOf { it.length() })
    }

    @Test
    fun `ignores an entry larger than the configured cache limit`() = withCache(maxBytes = 4L) { cache, directory ->
        cache.write("too-large", byteArrayOf(1, 2, 3, 4, 5))

        assertNull(cache.read("too-large"))
        assertTrue(directory.listFiles().isNullOrEmpty())
    }

    @Test
    fun `clear removes cached thumbnails`() = withCache { cache, directory ->
        cache.write("content://video/1", byteArrayOf(1, 2, 3))

        cache.clear()

        assertNull(cache.read("content://video/1"))
        assertTrue(directory.listFiles().isNullOrEmpty())
    }

    private fun withCache(
        maxBytes: Long = VideoThumbnailDiskCache.DEFAULT_MAX_BYTES,
        block: (VideoThumbnailDiskCache, java.io.File) -> Unit,
    ) {
        val directory = Files.createTempDirectory("veyra-thumbnail-cache").toFile()
        try {
            block(VideoThumbnailDiskCache(directory, maxBytes), directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
