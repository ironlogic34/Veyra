package com.ironlogic.veyra

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.ironlogic.veyra.data.video.VideoCompletionMode
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoThumbnailDataSource
import com.ironlogic.veyra.data.video.VideoMetadataRepository
import com.ironlogic.veyra.data.video.VideoLibraryRepository
import com.ironlogic.veyra.data.video.VideoLibrarySettings
import com.ironlogic.veyra.data.video.VideoLibrarySettingsStore
import com.ironlogic.veyra.data.video.VideoRelatedItems
import com.ironlogic.veyra.data.video.VideoContinueWatching
import com.ironlogic.veyra.data.video.VideoContinueWatchingEntry
import com.ironlogic.veyra.data.video.VideoPlaybackSettings
import com.ironlogic.veyra.data.video.VideoPlaybackSettingsStore
import com.ironlogic.veyra.data.video.VideoLibraryQuery
import com.ironlogic.veyra.data.video.VideoSortOrder
import com.ironlogic.veyra.permissions.VideoAccessLevel
import com.ironlogic.veyra.permissions.VideoAccessPermission
import com.ironlogic.veyra.playback.VideoPlaybackService
import com.ironlogic.veyra.playback.VideoPlaybackSession
import com.ironlogic.veyra.playback.VideoPlaybackSessionLifecycle
import com.ironlogic.veyra.ui.theme.VeyraTheme
import com.ironlogic.veyra.ui.video.components.VideoLibraryControls
import com.ironlogic.veyra.ui.video.components.VideoMimeTypeOption
import com.ironlogic.veyra.ui.video.components.VideoLibraryList
import com.ironlogic.veyra.ui.video.components.VideoPlaybackContent
import com.ironlogic.veyra.ui.video.components.VideoPlaybackSettingsContent
import com.ironlogic.veyra.ui.video.components.VideoContinueWatchingContent
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val playbackSessionState = mutableStateOf<VideoPlaybackSession?>(null)
    private var playbackLifecycle: VideoPlaybackSessionLifecycle? = null
    private var serviceBound = false

    private val playbackServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? VideoPlaybackService.LocalBinder ?: return
            val service = localBinder.getService()
            val session = service.playbackSession
            playbackSessionState.value = session
            playbackLifecycle?.close()
            playbackLifecycle = VideoPlaybackSessionLifecycle(
                lifecycle = lifecycle,
                session = session,
                ownerScope = playbackOwnerScope,
                shouldReleaseOnDestroy = { false }
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackLifecycle?.close()
            playbackLifecycle = null
            playbackSessionState.value = null
        }
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, VideoPlaybackService::class.java)
        startService(serviceIntent)
        serviceBound = bindService(
            serviceIntent,
            playbackServiceConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        if (serviceBound) {
            unbindService(playbackServiceConnection)
            serviceBound = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        playbackLifecycle?.close()
        playbackLifecycle = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VeyraTheme {
                var accessLevel by remember { mutableStateOf(VideoAccessPermission.evaluate(this)) }
                var folderUris by remember { mutableStateOf(readPersistedFolderUris()) }
                var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
                var activeVideo by remember { mutableStateOf<VideoItem?>(null) }
                var continueWatching by remember {
                    mutableStateOf<List<VideoContinueWatchingEntry<VideoItem>>>(emptyList())
                }
                var status by remember { mutableStateOf("Choose how Veyra can access your videos.") }
                var isLoading by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }
                var selectedMimeTypes by remember { mutableStateOf<Set<String>?>(null) }
                val librarySettingsStore = remember {
                    VideoLibrarySettingsStore(applicationContext)
                }
                var sortOrder by remember {
                    mutableStateOf(librarySettingsStore.load().sortOrder)
                }
                var showPlaybackSettings by remember { mutableStateOf(false) }
                val playbackSettingsStore = remember {
                    VideoPlaybackSettingsStore(applicationContext)
                }
                var playbackSettings by remember {
                    mutableStateOf(playbackSettingsStore.load())
                }
                val scope = rememberCoroutineScope()
                val playbackSession = playbackSessionState.value
                val thumbnailDataSource = remember { VideoThumbnailDataSource(applicationContext) }
                val metadataRepository = remember { VideoMetadataRepository(applicationContext) }
                val mimeTypeOptions = remember(videos) {
                    videos.mapNotNull { it.mimeType?.lowercase(Locale.ROOT) }
                        .distinct()
                        .sorted()
                        .map { mimeType ->
                            VideoMimeTypeOption(
                                label = videoMimeTypeLabel(mimeType),
                                mimeTypes = setOf(mimeType)
                            )
                        }
                }
                val visibleVideos = remember(videos, searchQuery, sortOrder, selectedMimeTypes) {
                    VideoLibraryQuery.apply(
                        items = videos,
                        searchQuery = searchQuery,
                        sortOrder = sortOrder,
                        allowedMimeTypes = selectedMimeTypes,
                        displayName = VideoItem::displayName,
                        relativePath = VideoItem::relativePath,
                        durationMs = VideoItem::durationMs,
                        sizeBytes = VideoItem::sizeBytes,
                        modifiedSeconds = VideoItem::dateModifiedSeconds,
                        mimeType = VideoItem::mimeType
                    )
                }

                fun leavePlayback() {
                    val session = playbackSession
                    scope.launch {
                        session?.engine?.pause()
                        try {
                            session?.saveProgress()
                        } catch (_: Exception) {
                            status = "Playback progress could not be saved."
                        } finally {
                            activeVideo = null
                        }
                    }
                }

                fun playVideo(video: VideoItem) {
                    val session = playbackSession
                    if (session == null) {
                        status = "Playback service is still starting. Try again in a moment."
                        return
                    }
                    scope.launch {
                        try {
                            val queue = videos.map { it.contentUri }
                            val names = videos.map { it.displayName }
                            val index = videos.indexOfFirst {
                                it.contentUri == video.contentUri
                            }.coerceAtLeast(0)
                            session.openQueue(
                                mediaUris = queue,
                                startIndex = index,
                                resume = playbackSettings.resumePlayback,
                                completionMode = playbackSettings.completionMode,
                                displayNames = names
                            )
                            activeVideo = video
                            status = "Playing ${video.displayName}"
                        } catch (_: Exception) {
                            status = "This video could not be opened. Its access may have changed."
                        }
                    }
                }

                fun updatePlaybackSettings(updated: VideoPlaybackSettings) {
                    playbackSettings = updated
                    playbackSettingsStore.save(updated)
                    playbackSession?.setCompletionMode(updated.completionMode)
                }

                BackHandler(enabled = activeVideo != null || showPlaybackSettings) {
                    if (activeVideo != null) leavePlayback() else showPlaybackSettings = false
                }

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
                        metadataRepository.clearCache()
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

                LaunchedEffect(videos, playbackSession) {
                    val currentUri = playbackSession?.engine?.currentMediaUri()
                    if (currentUri != null) {
                        activeVideo = videos.firstOrNull { it.contentUri == currentUri }
                    }
                }

                LaunchedEffect(playbackSession, videos, activeVideo) {
                    if (playbackSession == null || activeVideo != null || videos.isEmpty()) {
                        continueWatching = emptyList()
                    } else {
                        continueWatching = try {
                            VideoContinueWatching.select(
                                records = playbackSession.recentHistory(),
                                candidates = videos,
                                mediaUri = { it.contentUri.toString() }
                            )
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                }

                DisposableEffect(videos, playbackSession) {
                    val session = playbackSession
                    if (session == null) {
                        onDispose { }
                    } else {
                        val listener = object : Player.Listener {
                            override fun onMediaItemTransition(
                                mediaItem: androidx.media3.common.MediaItem?,
                                reason: Int
                            ) {
                                val currentUri = session.engine.currentMediaUri()
                                if (currentUri != null) {
                                    activeVideo = videos.firstOrNull {
                                        it.contentUri == currentUri
                                    }
                                }
                            }
                        }
                        session.engine.addListener(listener)
                        onDispose { session.engine.removeListener(listener) }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val selectedVideo = activeVideo
                    val relatedVideos = remember(videos, selectedVideo) {
                        selectedVideo?.let { current ->
                            VideoRelatedItems.select(
                                currentMediaUri = current.contentUri.toString(),
                                currentRelativePath = current.relativePath,
                                currentMimeType = current.mimeType,
                                candidates = videos,
                                mediaUri = { it.contentUri.toString() },
                                relativePath = { it.relativePath },
                                mimeType = { it.mimeType },
                                modifiedSeconds = { it.dateModifiedSeconds },
                                displayName = { it.displayName }
                            )
                        } ?: emptyList()
                    }
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
                            if (playbackSession != null) {
                                VideoPlaybackContent(
                                    session = playbackSession,
                                    relatedVideos = relatedVideos,
                                    onRelatedVideoClick = ::playVideo,
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                            } else {
                                Text("Connecting to playback service…")
                            }
                            Text(
                                "Playback continues when Veyra is in the background. Use the playback notification or Bluetooth controls.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else if (showPlaybackSettings) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Playback settings",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                OutlinedButton(onClick = { showPlaybackSettings = false }) {
                                    Text("Back")
                                }
                            }
                            VideoPlaybackSettingsContent(
                                settings = playbackSettings,
                                onSettingsChange = ::updatePlaybackSettings,
                                modifier = Modifier.fillMaxWidth()
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
                                OutlinedButton(onClick = { showPlaybackSettings = true }) {
                                    Text("Playback settings")
                                }
                                if (isLoading) Text("Scanning…")
                            }
                            Text(status, style = MaterialTheme.typography.bodySmall)
                            if (playbackSession == null) {
                                Text("Starting playback service…", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Videos (${visibleVideos.size} of ${videos.size}) · Tap to play",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (videos.isNotEmpty()) {
                                VideoLibraryControls(
                                    searchQuery = searchQuery,
                                    sortOrder = sortOrder,
                                    onSearchQueryChange = { searchQuery = it },
                                    onSortOrderChange = {
                                        sortOrder = it
                                        librarySettingsStore.save(VideoLibrarySettings(sortOrder = it))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    mimeTypeOptions = mimeTypeOptions,
                                    selectedMimeTypes = selectedMimeTypes,
                                    onSelectedMimeTypesChange = { selectedMimeTypes = it }
                                )
                            }
                            if (continueWatching.isNotEmpty()) {
                                VideoContinueWatchingContent(
                                    items = continueWatching,
                                    onVideoClick = ::playVideo,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (visibleVideos.isEmpty()) {
                                Text(
                                    if (videos.isEmpty()) {
                                        "No videos found in the accessible locations."
                                    } else {
                                        "No videos match your search."
                                    },
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                VideoLibraryList(
                                    videos = visibleVideos,
                                    thumbnailDataSource = thumbnailDataSource,
                                    metadataRepository = metadataRepository,
                                    onVideoClick = ::playVideo,
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalItemSpacing = 8
                                )
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
         * Lifecycle progress writes may outlive an Activity instance.
         * The service, not the Activity, owns final player release.
         */
        val playbackOwnerScope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate
        )
    }
}

private fun videoMimeTypeLabel(mimeType: String): String = when (mimeType.lowercase(Locale.ROOT)) {
    "video/mp4" -> "MP4"
    "video/x-matroska" -> "MKV"
    "video/webm" -> "WebM"
    "video/3gpp" -> "3GP"
    "video/quicktime" -> "MOV"
    "video/x-msvideo" -> "AVI"
    else -> mimeType.substringAfter('/').removePrefix("x-").uppercase(Locale.ROOT)
}
