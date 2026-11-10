package com.ironlogic.veyra

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ironlogic.veyra.data.video.VideoLibraryRepository
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoCompletionMode
import com.ironlogic.veyra.permissions.VideoAccessLevel
import com.ironlogic.veyra.permissions.VideoAccessPermission
import com.ironlogic.veyra.playback.VideoPlaybackSession
import com.ironlogic.veyra.playback.VideoPlaybackSessionLifecycle
import com.ironlogic.veyra.ui.theme.VeyraTheme
import com.ironlogic.veyra.ui.video.components.VideoPlaybackContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var playbackSession: VideoPlaybackSession
    private lateinit var playbackLifecycle: VideoPlaybackSessionLifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playbackSession = VideoPlaybackSession(applicationContext)
        playbackLifecycle = VideoPlaybackSessionLifecycle(
            lifecycle = lifecycle,
            session = playbackSession,
            ownerScope = playbackOwnerScope,
            shouldReleaseOnDestroy = { true }
        )

        setContent {
            VeyraTheme {
                var accessLevel by remember { mutableStateOf(VideoAccessPermission.evaluate(this)) }
                var folderUris by remember { mutableStateOf(readPersistedFolderUris()) }
                var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
                var activeVideo by remember { mutableStateOf<VideoItem?>(null) }
                var status by remember { mutableStateOf("Choose how Veyra can access your videos.") }
                var isLoading by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                fun leavePlayback() {
                    playbackSession.engine.pause()
                    activeVideo = null
                }

                BackHandler(enabled = activeVideo != null) { leavePlayback() }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    accessLevel = VideoAccessPermission.evaluate(this)
                    status = when (accessLevel) {
                        VideoAccessLevel.FULL_LIBRARY -> "Full video-library access granted."
                        VideoAccessLevel.SELECTED_VIDEOS -> "Access is limited to the videos you selected."
                        VideoAccessLevel.NONE -> "Video permission was not granted. You can still add a folder."
                    }
                }

                val folderPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    if (uri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            folderUris = readPersistedFolderUris()
                            status = "Folder access saved. Refresh to scan its videos."
                        } catch (_: SecurityException) {
                            status = "Android could not save access to that folder. Select it again."
                        }
                    }
                }

                fun refreshLibrary() {
                    scope.launch {
                        isLoading = true
                        try {
                            val result = VideoLibraryRepository(contentResolver).load(
                                accessLevel = accessLevel,
                                folderUris = folderUris
                            )
                            videos = result.videos
                            accessLevel = VideoAccessPermission.evaluate(this@MainActivity)
                            status = when {
                                result.errors.isNotEmpty() -> result.errors.joinToString(" ")
                                videos.isEmpty() -> "No videos found in the accessible locations."
                                else -> "Found ${videos.size} video(s)."
                            }
                        } catch (_: Exception) {
                            status = "The video library could not be loaded. Try refreshing."
                        } finally {
                            isLoading = false
                        }
                    }
                }

                LaunchedEffect(accessLevel, folderUris) { refreshLibrary() }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val selectedVideo = activeVideo
                    if (selectedVideo != null) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    selectedVideo.displayName,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                OutlinedButton(onClick = { leavePlayback() }) { Text("Back") }
                            }
                            VideoPlaybackContent(
                                session = playbackSession,
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                            Text(
                                "Playback progress is saved automatically. Returning to the library pauses playback.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Veyra · Video access", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                when (accessLevel) {
                                    VideoAccessLevel.FULL_LIBRARY -> "Device library: full access"
                                    VideoAccessLevel.SELECTED_VIDEOS -> "Device library: selected videos only"
                                    VideoAccessLevel.NONE -> "Device library: permission not granted"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    permissionLauncher.launch(VideoAccessPermission.requiredPermissions())
                                }) {
                                    Text(if (accessLevel == VideoAccessLevel.NONE) "Grant video access" else "Manage access")
                                }
                                OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Add folder") }
                            }
                            Text("Saved folders: ${folderUris.size}")
                            folderUris.forEach { folderUri ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        folderUri.lastPathSegment ?: "Selected folder",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    OutlinedButton(onClick = {
                                        try {
                                            contentResolver.releasePersistableUriPermission(
                                                folderUri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            )
                                        } catch (_: SecurityException) {
                                            // The provider or user may already have revoked the grant.
                                        }
                                        folderUris = readPersistedFolderUris()
                                    }) { Text("Remove") }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    accessLevel = VideoAccessPermission.evaluate(this@MainActivity)
                                    folderUris = readPersistedFolderUris()
                                    refreshLibrary()
                                }) { Text("Refresh") }
                                if (isLoading) Text("Scanning…")
                            }
                            Text(status, style = MaterialTheme.typography.bodySmall)
                            Text("Videos (${videos.size}) · Tap to play", style = MaterialTheme.typography.titleMedium)
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(videos, key = { it.contentUri.toString() }) { video ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    try {
                                                        val queue = videos.map { it.contentUri }
                                                        val index = videos.indexOfFirst {
                                                            it.contentUri == video.contentUri
                                                        }.coerceAtLeast(0)
                                                        playbackSession.openQueue(
                                                            mediaUris = queue,
                                                            startIndex = index,
                                                            resume = true,
                                                            completionMode = VideoCompletionMode.PLAY_NEXT
                                                        )
                                                        activeVideo = video
                                                        status = "Playing ${video.displayName}"
                                                    } catch (_: Exception) {
                                                        status = "This video could not be opened. Its access may have changed."
                                                    }
                                                }
                                            }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(video.displayName, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            video.relativePath ?: "Device library",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun readPersistedFolderUris(): List<Uri> =
        contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri }
            .distinct()

    private companion object {
        /**
         * Outlives an Activity instance so lifecycle-triggered final progress
         * saves and player release can finish during configuration changes.
         */
        val playbackOwnerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
