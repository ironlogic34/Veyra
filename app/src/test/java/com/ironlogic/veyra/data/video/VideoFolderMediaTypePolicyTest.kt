package com.ironlogic.veyra.data.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFolderMediaTypePolicyTest {
    @Test
    fun `accepts video MIME types regardless of case or parameters`() {
        assertTrue(VideoFolderMediaTypePolicy.isVideo("VIDEO/MP4; codecs=h264", "clip.bin"))
    }

    @Test
    fun `recognizes common video extensions when provider omits MIME type`() {
        assertTrue(VideoFolderMediaTypePolicy.isVideo(null, "film.MKV"))
        assertTrue(VideoFolderMediaTypePolicy.isVideo("application/octet-stream", "clip.mp4"))
    }

    @Test
    fun `rejects non-video documents`() {
        assertFalse(VideoFolderMediaTypePolicy.isVideo("image/jpeg", "photo.jpg"))
        assertFalse(VideoFolderMediaTypePolicy.isVideo(null, "notes.txt"))
    }
}
