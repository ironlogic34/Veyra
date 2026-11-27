package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFolderScanPolicyTest {
    @Test
    fun `root open security failures invalidate cached access`() {
        assertEquals(
            VideoFolderScanOutcome.ACCESS_LOST,
            VideoFolderScanPolicy.outcomeForRootOpenFailure(SecurityException("permission revoked")),
        )
    }

    @Test
    fun `transient root open failures preserve cached access`() {
        assertEquals(
            VideoFolderScanOutcome.INCOMPLETE,
            VideoFolderScanPolicy.outcomeForRootOpenFailure(IllegalStateException("provider unavailable")),
        )
    }

    @Test
    fun `incomplete root scan preserves its previous videos and directories`() {
        val existing = listOf(
            video("content://tree-a/old.mp4", setOf("tree-a"), setOf("tree-a", "tree-a/old")),
            video(
                "content://shared/video.mp4",
                setOf("tree-a", "tree-b"),
                setOf("tree-a", "tree-a/old", "tree-b", "tree-b/child"),
            ),
        )
        val directories = listOf(
            directory("tree-a/old", "tree-a", "tree-a"),
            directory("tree-b/child", "tree-b", "tree-b"),
        )
        val partial = video("content://tree-a/partial.mp4", setOf("tree-a"))

        val result = VideoFolderScanPolicy.commitRoot(
            existingVideos = existing,
            existingDirectories = directories,
            rootVideos = listOf(partial),
            rootDirectories = listOf(directory("tree-a/new", "tree-a", "tree-a")),
            rootUri = "tree-a",
            outcome = VideoFolderScanOutcome.INCOMPLETE,
        )

        assertEquals(existing, result.videos)
        assertEquals(directories, result.directories)
    }

    @Test
    fun `completed root replaces stale entries and preserves other root memberships`() {
        val existing = listOf(
            video("content://tree-a/old.mp4", setOf("tree-a"), setOf("tree-a", "tree-a/old")),
            video(
                "content://shared/video.mp4",
                setOf("tree-a", "tree-b"),
                setOf("tree-a", "tree-a/old", "tree-b", "tree-b/child"),
            ),
            video("content://tree-b/only.mp4", setOf("tree-b"), setOf("tree-b")),
        )
        val directories = listOf(
            directory("tree-a/old", "tree-a", "tree-a"),
            directory("tree-b/child", "tree-b", "tree-b"),
        )
        val refreshedShared = video(
            "content://shared/video.mp4",
            setOf("tree-a"),
            setOf("tree-a", "tree-a/new"),
        )
        val refreshed = listOf(
            video("content://tree-a/new.mp4", setOf("tree-a")),
            refreshedShared,
        )

        val result = VideoFolderScanPolicy.commitRoot(
            existingVideos = existing,
            existingDirectories = directories,
            rootVideos = refreshed,
            rootDirectories = listOf(directory("tree-a/new", "tree-a", "tree-a")),
            rootUri = "tree-a",
            outcome = VideoFolderScanOutcome.COMPLETE,
        )

        assertEquals(
            setOf("content://shared/video.mp4", "content://tree-b/only.mp4", "content://tree-a/new.mp4"),
            result.videos.map { it.contentUri }.toSet(),
        )
        val shared = result.videos.single { it.contentUri == "content://shared/video.mp4" }
        assertEquals(setOf("tree-a", "tree-b"), shared.sourceFolderUris)
        assertEquals(setOf("tree-a", "tree-a/new", "tree-b", "tree-b/child"), shared.sourceDirectoryUris)
        assertEquals(setOf("tree-a/new", "tree-b/child"), result.directories.map { it.uri }.toSet())
    }

    @Test
    fun `access loss removes only that root membership`() {
        val shared = video(
            "content://shared/video.mp4",
            setOf("tree-a", "tree-b"),
            setOf("tree-a", "tree-a/child", "tree-b", "tree-b/child"),
        )
        val onlyA = video("content://tree-a/only.mp4", setOf("tree-a"))
        val onlyB = video("content://tree-b/only.mp4", setOf("tree-b"))
        val directories = listOf(
            directory("tree-a/child", "tree-a", "tree-a"),
            directory("tree-b/child", "tree-b", "tree-b"),
        )

        val result = VideoFolderScanPolicy.commitRoot(
            existingVideos = listOf(shared, onlyA, onlyB),
            existingDirectories = directories,
            rootVideos = listOf(video("content://partial.mp4", setOf("tree-a"))),
            rootDirectories = emptyList(),
            rootUri = "tree-a",
            outcome = VideoFolderScanOutcome.ACCESS_LOST,
        )

        assertEquals(setOf(shared.contentUri, onlyB.contentUri), result.videos.map { it.contentUri }.toSet())
        assertEquals(setOf("tree-b"), result.videos.single { it.contentUri == shared.contentUri }.sourceFolderUris)
        assertEquals(setOf("tree-b", "tree-b/child"), result.videos.single { it.contentUri == shared.contentUri }.sourceDirectoryUris)
        assertEquals(setOf("tree-b/child"), result.directories.map { it.uri }.toSet())
    }

    @Test
    fun `removing a selected root prunes only that root from the cached snapshot`() {
        val shared = video(
            "content://shared/video.mp4",
            setOf("tree-a", "tree-b"),
            setOf("tree-a", "tree-a/child", "tree-b", "tree-b/child"),
        )
        val onlyA = video("content://tree-a/only.mp4", setOf("tree-a"))
        val onlyB = video("content://tree-b/only.mp4", setOf("tree-b"))
        val directories = listOf(
            directory("tree-a/child", "tree-a", "tree-a"),
            directory("tree-b/child", "tree-b", "tree-b"),
        )

        val result = VideoFolderScanPolicy.retainSelectedRoots(
            existingVideos = listOf(shared, onlyA, onlyB),
            existingDirectories = directories,
            selectedRootUris = listOf("tree-b"),
        )

        assertEquals(setOf(shared.contentUri, onlyB.contentUri), result.videos.map { it.contentUri }.toSet())
        assertEquals(setOf("tree-b"), result.videos.single { it.contentUri == shared.contentUri }.sourceFolderUris)
        assertEquals(setOf("tree-b", "tree-b/child"), result.videos.single { it.contentUri == shared.contentUri }.sourceDirectoryUris)
        assertEquals(listOf(directories.last()), result.directories)
    }

    private fun video(
        uri: String,
        roots: Set<String>,
        directories: Set<String> = roots,
    ) = VideoItem(
        id = uri,
        contentUri = uri,
        displayName = "Video.mp4",
        durationMs = 1_000L,
        sizeBytes = 1L,
        addedEpochSeconds = 1L,
        sourceFolderUris = roots,
        sourceDirectoryUris = directories,
    )

    private fun directory(uri: String, parent: String, root: String) = VideoFolderDirectory(
        uri = uri,
        parentUri = parent,
        rootUri = root,
        name = "Folder",
    )
}
