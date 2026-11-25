package com.ironlogic.veyra.ui.video

import android.app.Activity
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ironlogic.veyra.playback.VeyraPlaybackService
import com.ironlogic.veyra.playback.VideoSleepTimerStore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoQueuePolicy
import com.ironlogic.veyra.data.video.VideoMemoryMarkStore
import com.ironlogic.veyra.data.video.VideoPlaybackProgressStore
import com.ironlogic.veyra.data.video.VideoAutoResumePolicy
import com.ironlogic.veyra.data.video.VideoPlaybackHistoryStore
import com.ironlogic.veyra.data.video.VideoPlaybackSettingsStore
import com.ironlogic.veyra.data.video.VideoOrientationMode
import com.ironlogic.veyra.data.video.VideoResizeMode
import com.ironlogic.veyra.data.video.VideoRepeatMode
import com.ironlogic.veyra.data.video.VideoSubtitleSelection
import com.ironlogic.veyra.data.video.VideoSubtitleStore
import com.ironlogic.veyra.data.video.VideoSubtitlePermissionPolicy
import com.ironlogic.veyra.data.video.VideoSubtitleTypePolicy
import com.ironlogic.veyra.data.video.VideoSleepTimerPolicy
import com.ironlogic.veyra.data.video.VideoPlaybackLockPolicy
import com.ironlogic.veyra.data.video.VideoLockGesture
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    queue: List<VideoItem>,
    onQueueChange: (List<VideoItem>) -> Unit,
    onVideoChanged: (VideoItem) -> Unit,
    progressStore: VideoPlaybackProgressStore,
    historyStore: VideoPlaybackHistoryStore,
    settingsStore: VideoPlaybackSettingsStore,
    isTabletLandscape: Boolean,
    subtitleStore: VideoSubtitleStore,
    memoryMarkStore: VideoMemoryMarkStore,
    initialPositionMs: Long? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var connectionFailed by remember { mutableStateOf(false) }
    var connectionAttempt by remember { mutableStateOf(0) }

    DisposableEffect(context, connectionAttempt) {
        var disposed = false
        controller = null
        connectionFailed = false
        val token = SessionToken(
            context,
            ComponentName(context, VeyraPlaybackService::class.java),
        )
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                if (!disposed) {
                    val connected = runCatching { future.get() }.getOrNull()
                    controller = connected
                    connectionFailed = connected == null
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            disposed = true
            MediaController.releaseFuture(future)
            controller = null
        }
    }

    val connectedController = controller
    if (connectedController == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (connectionFailed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Veyra couldn't connect to the player service.", color = Color.White)
                    TextButton(onClick = { connectionAttempt += 1 }) { Text("Retry") }
                }
            } else {
                Text("Connecting to Veyra player…", color = Color.White)
            }
        }
    } else {
        ConnectedVideoPlayerScreen(
            video = video,
            queue = queue,
            onQueueChange = onQueueChange,
            onVideoChanged = onVideoChanged,
            player = connectedController,
            progressStore = progressStore,
            historyStore = historyStore,
            settingsStore = settingsStore,
            isTabletLandscape = isTabletLandscape,
            subtitleStore = subtitleStore,
            memoryMarkStore = memoryMarkStore,
            initialPositionMs = initialPositionMs,
            onBack = onBack,
        )
    }
}

@Composable
private fun ConnectedVideoPlayerScreen(
    video: VideoItem,
    queue: List<VideoItem>,
    onQueueChange: (List<VideoItem>) -> Unit,
    onVideoChanged: (VideoItem) -> Unit,
    player: MediaController,
    progressStore: VideoPlaybackProgressStore,
    historyStore: VideoPlaybackHistoryStore,
    settingsStore: VideoPlaybackSettingsStore,
    isTabletLandscape: Boolean,
    subtitleStore: VideoSubtitleStore,
    memoryMarkStore: VideoMemoryMarkStore,
    initialPositionMs: Long? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val sleepTimerStore = remember(context) { VideoSleepTimerStore(context) }
    var resizeMode by remember(video.contentUri) {
        mutableStateOf(settingsStore.readResizeMode())
    }
    var repeatMode by remember(video.contentUri) {
        mutableStateOf(settingsStore.readRepeatMode())
    }
    val orientationMode = remember(video.contentUri) {
        settingsStore.readOrientationMode()
    }
    val initialSubtitle = remember(video.contentUri) {
        readAvailableSubtitle(context, subtitleStore, video)
    }
    var selectedSubtitle by remember(video.contentUri) { mutableStateOf(initialSubtitle) }
    var subtitleMessage by remember(video.contentUri) { mutableStateOf<String?>(null) }
    val activity = context as? Activity
    val originalRequestedOrientation = remember(activity) { activity?.requestedOrientation }

    DisposableEffect(activity, orientationMode) {
        activity?.requestedOrientation = orientationMode.toRequestedOrientation()
        onDispose {
            if (activity != null && originalRequestedOrientation != null) {
                activity.requestedOrientation = originalRequestedOrientation
            }
        }
    }

    LaunchedEffect(player, video.contentUri, queue, initialSubtitle, initialPositionMs) {
        val playlist = VideoQueuePolicy.playlist(queue, video)
        val selectedIndex = playlist.indexOfFirst { it.contentUri == video.contentUri }.coerceAtLeast(0)
        val desiredIds = playlist.map { it.contentUri }
        val currentIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        val currentMatches = player.currentMediaItem?.mediaId == video.contentUri

        if (!currentMatches || currentIds != desiredIds) {
            val startPositionMs = initialPositionMs?.coerceAtLeast(0L)
                ?: if (currentMatches) {
                    player.currentPosition
                } else {
                    VideoAutoResumePolicy.startPositionMs(
                        autoResumeEnabled = settingsStore.readAutoResumeEnabled(),
                        manualPositionMs = null,
                        savedPositionMs = progressStore.read(video)?.positionMs,
                    )
                }
            val wasPlaying = player.playWhenReady
            val mediaItems = playlist.map { item ->
                val subtitle = if (item.contentUri == video.contentUri) {
                    initialSubtitle
                } else {
                    readAvailableSubtitle(context, subtitleStore, item)
                }
                createMediaItem(item, subtitle)
            }
            player.setMediaItems(mediaItems, selectedIndex, startPositionMs)
            player.prepare()
            player.playWhenReady = if (currentMatches) wasPlaying else true
        } else if (initialPositionMs != null) {
            player.seekTo(initialPositionMs.coerceAtLeast(0L))
        }
    }
    LaunchedEffect(player, repeatMode) {
        player.repeatMode = repeatMode.toPlayerRepeatMode()
    }

    var controlsVisible by remember(video.contentUri) { mutableStateOf(true) }
    var isPlaying by remember(video.contentUri) { mutableStateOf(true) }
    var isSeeking by remember(video.contentUri) { mutableStateOf(false) }
    var currentPositionMs by remember(video.contentUri) { mutableStateOf(0L) }
    var durationMs by remember(video.contentUri) { mutableStateOf(video.durationMs) }
    var seekPreviewMs by remember(video.contentUri) { mutableStateOf(0L) }
    val initialSleepTimerRemainingMs = remember(video.contentUri) {
        sleepTimerStore.remainingMs() ?: 0L
    }
    var sleepTimerActive by remember(video.contentUri) {
        mutableStateOf(initialSleepTimerRemainingMs > 0L)
    }
    var sleepTimerRemainingMs by remember(video.contentUri) {
        mutableStateOf(initialSleepTimerRemainingMs)
    }
    var showSleepTimerDialog by remember(video.contentUri) { mutableStateOf(false) }
    var showQueueDialog by remember(video.contentUri) { mutableStateOf(false) }
    var controlsLocked by remember(video.contentUri) { mutableStateOf(false) }
    var playbackError by remember(video.contentUri) { mutableStateOf<String?>(null) }
    var memoryMarks by remember(video.contentUri) {
        mutableStateOf(memoryMarkStore.readForVideo(video.contentUri))
    }
    var showMemoryMarksDialog by remember(video.contentUri) { mutableStateOf(false) }
    var showAddMemoryMarkDialog by remember(video.contentUri) { mutableStateOf(false) }
    var memoryMarkNote by remember(video.contentUri) { mutableStateOf("") }
    var memoryMarkMood by remember(video.contentUri) { mutableStateOf<String?>(null) }
    var customMemoryMarkMood by remember(video.contentUri) { mutableStateOf("") }

    fun saveAndExit() {
        val currentUri = player.currentMediaItem?.mediaId
        val currentVideo = queue.firstOrNull { it.contentUri == currentUri } ?: video
        val duration = player.duration.takeIf { it > 0L } ?: currentVideo.durationMs
        progressStore.save(currentVideo, player.currentPosition, duration)
        historyStore.record(currentVideo, player.currentPosition, duration)
        onBack()
    }

    fun applySubtitle(selection: VideoSubtitleSelection?) {
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex < 0) return
        val wasReadyToPlay = player.playWhenReady
        val position = player.currentPosition
        player.replaceMediaItem(currentIndex, createMediaItem(video, selection))
        player.seekTo(currentIndex, position)
        player.prepare()
        player.playWhenReady = wasReadyToPlay
        selectedSubtitle = selection
    }

    fun clearUpcoming() {
        val currentUri = player.currentMediaItem?.mediaId ?: video.contentUri
        val updatedQueue = VideoQueuePolicy.clearUpcoming(queue, currentUri)
        if (updatedQueue == queue) return

        val currentIndex = player.currentMediaItemIndex
        if (currentIndex >= 0) {
            for (index in player.mediaItemCount - 1 downTo currentIndex + 1) {
                player.removeMediaItem(index)
            }
        }
        onQueueChange(updatedQueue)
        controlsVisible = true
    }

    fun removeFromQueue(item: VideoItem) {
        val currentUri = player.currentMediaItem?.mediaId ?: video.contentUri
        val updatedQueue = VideoQueuePolicy.remove(queue, currentUri, item.contentUri)
        if (updatedQueue == queue) return

        val playerIndex = (0 until player.mediaItemCount).firstOrNull { index ->
            player.getMediaItemAt(index).mediaId == item.contentUri
        }
        if (playerIndex != null) player.removeMediaItem(playerIndex)
        onQueueChange(updatedQueue)
        controlsVisible = true
    }

    fun enqueueNext(item: VideoItem) {
        val currentUri = player.currentMediaItem?.mediaId ?: video.contentUri
        val updatedQueue = VideoQueuePolicy.playNext(queue, currentUri, item)
        if (updatedQueue == queue) {
            subtitleMessage = when {
                item.contentUri == currentUri -> "This video is already playing."
                else -> "This video is already next."
            }
            controlsVisible = true
            return
        }

        val existingIndex = (0 until player.mediaItemCount).firstOrNull { index ->
            player.getMediaItemAt(index).mediaId == item.contentUri
        }
        if (existingIndex != null) player.removeMediaItem(existingIndex)

        val currentIndex = player.currentMediaItemIndex
        if (currentIndex < 0) {
            subtitleMessage = "The current playback queue is unavailable."
            return
        }
        val insertionIndex = (currentIndex + 1).coerceIn(0, player.mediaItemCount)
        player.addMediaItem(
            insertionIndex,
            createMediaItem(item, readAvailableSubtitle(context, subtitleStore, item)),
        )
        onQueueChange(updatedQueue)
        subtitleMessage = "${item.displayName} will play next."
        controlsVisible = true
        showQueueDialog = false
    }

    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val displayName = querySubtitleDisplayName(context, uri)
            val mimeType = VideoSubtitleTypePolicy.mimeTypeFor(displayName)
            if (mimeType == null) {
                subtitleMessage = "Choose an .srt or .vtt subtitle file."
            } else {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    val previousSelection = selectedSubtitle
                    val selection = VideoSubtitleSelection(uri.toString(), displayName)
                    subtitleStore.write(video.contentUri, selection)
                    applySubtitle(selection)
                    VideoSubtitlePermissionPolicy.uriToRelease(
                        previousSelection?.uri,
                        selection.uri,
                    )?.let { oldUri ->
                        releasePersistedSubtitleAccess(context, oldUri)
                    }
                    subtitleMessage = "Loaded $displayName"
                } catch (_: SecurityException) {
                    subtitleMessage = "Veyra couldn't keep access to that subtitle. Choose it again."
                }
            }
        }
    }

    BackHandler {
        if (controlsLocked) {
            controlsLocked = false
            controlsVisible = true
        } else {
            saveAndExit()
        }
    }

    LaunchedEffect(player, isSeeking) {
        while (true) {
            durationMs = player.duration.takeIf { it > 0L } ?: video.durationMs
            isPlaying = player.isPlaying
            if (!isSeeking) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                seekPreviewMs = currentPositionMs
            }
            delay(250)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, isSeeking, isTabletLandscape) {
        if (!isTabletLandscape && controlsVisible && isPlaying && !isSeeking) {
            delay(3_500)
            controlsVisible = false
        }
    }

    LaunchedEffect(player, sleepTimerActive) {
        if (!sleepTimerActive) {
            sleepTimerRemainingMs = 0L
            return@LaunchedEffect
        }
        while (true) {
            val remaining = sleepTimerStore.remainingMs() ?: 0L
            sleepTimerRemainingMs = remaining
            if (remaining == 0L) {
                sleepTimerActive = false
                player.pause()
                controlsVisible = true
                break
            }
            delay(minOf(1_000L, remaining))
        }
    }

    DisposableEffect(player, video.contentUri, queue, onVideoChanged) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val nextVideo = queue.firstOrNull { it.contentUri == mediaItem?.mediaId }
                if (nextVideo != null && nextVideo.contentUri != video.contentUri) {
                    val resumePositionMs = VideoAutoResumePolicy.startPositionMs(
                        autoResumeEnabled = settingsStore.readAutoResumeEnabled(),
                        manualPositionMs = null,
                        savedPositionMs = progressStore.read(nextVideo)?.positionMs,
                    )
                    if (resumePositionMs > 0L) {
                        player.seekTo(player.currentMediaItemIndex, resumePositionMs)
                    }
                    onVideoChanged(nextVideo)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = if (error.cause is SecurityException) {
                    "Veyra no longer has permission to read this video. Return to the library and restore access."
                } else {
                    "Veyra couldn't play this video. Check that the file is available and that its format is supported."
                }
                controlsLocked = false
                controlsVisible = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) playbackError = null
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            val currentUri = player.currentMediaItem?.mediaId
            val currentVideo = queue.firstOrNull { it.contentUri == currentUri } ?: video
            if (player.playbackState == Player.STATE_ENDED) {
                progressStore.clear(currentVideo)
            } else {
                val duration = player.duration.takeIf { it > 0L } ?: currentVideo.durationMs
                progressStore.save(currentVideo, player.currentPosition, duration)
                historyStore.record(currentVideo, player.currentPosition, duration)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = isTabletLandscape
                    controllerAutoShow = true
                    this.resizeMode = resizeMode.toPlayerViewResizeMode()
                }
            },
            update = { view ->
                view.player = player
                view.useController = isTabletLandscape
                view.resizeMode = resizeMode.toPlayerViewResizeMode()
            },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isTabletLandscape) Modifier else Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { controlsVisible = true })
                    },
                ),
        )

        if (isTabletLandscape && !controlsLocked) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = ::saveAndExit) { Text("Back") }
                Button(onClick = { showQueueDialog = true }) { Text("Queue (${queue.size})") }
            }
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "application/octet-stream")) }) {
                    Text("Subtitles")
                }
                Button(onClick = { showSleepTimerDialog = true }) {
                    Text(sleepTimerLabel(sleepTimerRemainingMs))
                }
                Button(onClick = { showMemoryMarksDialog = true }) {
                    Text("Marks (${memoryMarks.size})")
                }
                Button(
                    onClick = {
                        repeatMode = repeatMode.next()
                        settingsStore.writeRepeatMode(repeatMode)
                    },
                ) {
                    Text("Repeat: ${repeatMode.label}")
                }
                Button(onClick = {
                    controlsLocked = true
                    controlsVisible = false
                }) {
                    Text("Lock")
                }
                if (selectedSubtitle != null) {
                    Button(onClick = {
                        val previousUri = selectedSubtitle?.uri
                        subtitleStore.clear(video.contentUri)
                        applySubtitle(null)
                        releasePersistedSubtitleAccess(context, previousUri)
                        subtitleMessage = "Subtitles off"
                    }) { Text("Off") }
                }
                Button(
                    onClick = {
                        resizeMode = resizeMode.next()
                        settingsStore.writeResizeMode(resizeMode)
                    },
                ) {
                    Text("Aspect: ${resizeMode.label}")
                }
            }
            subtitleMessage?.let {
                Text(
                    it,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    color = Color.White,
                )
            }
        } else if (controlsVisible && !controlsLocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = ::saveAndExit) { Text("Back") }
                    Text(
                        video.displayName,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(
                        onClick = {
                            resizeMode = resizeMode.next()
                            settingsStore.writeResizeMode(resizeMode)
                        },
                    ) {
                        Text(resizeMode.label)
                    }
                    TextButton(
                        onClick = {
                            repeatMode = repeatMode.next()
                            settingsStore.writeRepeatMode(repeatMode)
                        },
                    ) {
                        Text("Repeat: ${repeatMode.label}")
                    }
                    TextButton(
                        onClick = {
                            controlsLocked = true
                            controlsVisible = false
                        },
                    ) {
                        Text("Lock")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "application/octet-stream"))
                            },
                        ) {
                            Text(selectedSubtitle?.displayName ?: "Subtitles")
                        }
                        TextButton(onClick = { showSleepTimerDialog = true }) {
                            Text(sleepTimerLabel(sleepTimerRemainingMs))
                        }
                        TextButton(onClick = { showMemoryMarksDialog = true }) {
                            Text("Marks (${memoryMarks.size})")
                        }
                        TextButton(onClick = { showQueueDialog = true }) {
                            Text("Queue")
                        }
                        if (selectedSubtitle != null) {
                            TextButton(
                                onClick = {
                                    val previousUri = selectedSubtitle?.uri
                                    subtitleStore.clear(video.contentUri)
                                    applySubtitle(null)
                                    releasePersistedSubtitleAccess(context, previousUri)
                                    subtitleMessage = "Subtitles off"
                                },
                            ) { Text("Off") }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    subtitleMessage?.let {
                        Text(it, color = Color.White, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatPlaybackTime(if (isSeeking) seekPreviewMs else currentPositionMs), color = Color.White, fontSize = 12.sp)
                        Text(formatPlaybackTime(durationMs), color = Color.White, fontSize = 12.sp)
                    }
                    Slider(
                        value = if (durationMs > 0L) {
                            ((if (isSeeking) seekPreviewMs else currentPositionMs).toFloat() / durationMs).coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                        onValueChange = { fraction ->
                            isSeeking = true
                            seekPreviewMs = (fraction * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                        },
                        onValueChangeFinished = {
                            player.seekTo(seekPreviewMs)
                            currentPositionMs = seekPreviewMs
                            isSeeking = false
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            enabled = player.hasPreviousMediaItem(),
                            onClick = {
                                player.seekToPreviousMediaItem()
                                controlsVisible = true
                            },
                        ) { Text("Previous") }
                        TextButton(
                            onClick = {
                                if (player.isPlaying) player.pause() else player.play()
                                controlsVisible = true
                            },
                        ) {
                            Text(
                                if (isPlaying) "Pause" else "Play",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                        }
                        TextButton(
                            enabled = player.hasNextMediaItem(),
                            onClick = {
                                player.seekToNextMediaItem()
                                controlsVisible = true
                            },
                        ) { Text("Next") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                                controlsVisible = true
                            },
                        ) { Text("−10s") }
                        Spacer(modifier = Modifier.width(40.dp))
                        TextButton(
                            onClick = {
                                val target = (player.currentPosition + 10_000L).coerceAtMost(durationMs)
                                player.seekTo(target)
                                controlsVisible = true
                            },
                        ) { Text("+10s") }
                    }
                }
            }
        }

        if (controlsLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f))
                    .pointerInput(controlsLocked) {
                        detectTapGestures(
                            onTap = {
                                if (VideoPlaybackLockPolicy.shouldUnlock(VideoLockGesture.TAP)) {
                                    controlsLocked = false
                                    controlsVisible = true
                                }
                            },
                            onLongPress = {
                                if (VideoPlaybackLockPolicy.shouldUnlock(VideoLockGesture.LONG_PRESS)) {
                                    controlsLocked = false
                                    controlsVisible = true
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Controls locked", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Hold anywhere to unlock", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }

        playbackError?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Unable to play this video", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(message, color = Color.LightGray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = ::saveAndExit) { Text("Back to library") }
                        Button(
                            onClick = {
                                player.prepare()
                                player.playWhenReady = true
                            },
                        ) { Text("Retry") }
                    }
                }
            }
        }
    }

    if (showQueueDialog) {
        val currentUri = player.currentMediaItem?.mediaId
        val currentQueueIndex = queue.indexOfFirst { it.contentUri == currentUri }
        AlertDialog(
            onDismissRequest = { showQueueDialog = false },
            title = { Text("Playback queue") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${queue.size} videos in this session", color = Color.LightGray)
                        TextButton(
                            enabled = currentQueueIndex >= 0 && currentQueueIndex < queue.lastIndex,
                            onClick = ::clearUpcoming,
                        ) { Text("Clear upcoming") }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(queue, key = { _, item -> item.contentUri }) { index, item ->
                            val isCurrent = item.contentUri == currentUri
                            val status = when {
                                isCurrent -> "Now playing"
                                index > currentQueueIndex -> "Up next"
                                else -> "Earlier in queue"
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF211E25), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    item.displayName,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                                Text(status, color = Color.LightGray, fontSize = 11.sp)
                                if (!isCurrent) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                val targetIndex = (0 until player.mediaItemCount).firstOrNull { playerIndex ->
                                                    player.getMediaItemAt(playerIndex).mediaId == item.contentUri
                                                }
                                                if (targetIndex != null) {
                                                    player.seekTo(targetIndex, 0L)
                                                    player.playWhenReady = true
                                                    controlsVisible = true
                                                    showQueueDialog = false
                                                }
                                            },
                                        ) { Text("Play now") }
                                        TextButton(onClick = { enqueueNext(item) }) {
                                            Text("Play next")
                                        }
                                        TextButton(onClick = { removeFromQueue(item) }) {
                                            Text("Remove")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQueueDialog = false }) { Text("Close") }
            },
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    VideoSleepTimerPolicy.PRESET_MINUTES.forEach { minutes ->
                        TextButton(
                            onClick = {
                                val duration = VideoSleepTimerPolicy.durationMs(minutes)
                                if (duration != null) {
                                    sleepTimerStore.schedule(duration)
                                    sleepTimerActive = true
                                }
                                showSleepTimerDialog = false
                            },
                        ) {
                            Text("$minutes minutes")
                        }
                    }
                    if (sleepTimerActive) {
                        TextButton(
                            onClick = {
                                sleepTimerStore.clear()
                                sleepTimerActive = false
                                showSleepTimerDialog = false
                            },
                        ) {
                            Text("Cancel timer")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close")
                }
            },
        )
    }

    if (showMemoryMarksDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryMarksDialog = false },
            title = { Text("Memory Marks") },
            text = {
                if (memoryMarks.isEmpty()) {
                    Text("Save a moment from this video and it will stay here.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(memoryMarks, key = { it.id }) { mark ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(formatPlaybackTime(mark.positionMs), fontWeight = FontWeight.SemiBold)
                                    if (mark.note.isNotBlank()) Text(mark.note, fontSize = 12.sp)
                                    mark.mood?.let { Text(it, fontSize = 11.sp) }
                                }
                                TextButton(
                                    onClick = {
                                        player.seekTo(mark.positionMs)
                                        currentPositionMs = mark.positionMs
                                        seekPreviewMs = mark.positionMs
                                        controlsVisible = true
                                        showMemoryMarksDialog = false
                                    },
                                ) { Text("Jump") }
                                TextButton(
                                    onClick = {
                                        memoryMarkStore.remove(mark.id)
                                        memoryMarks = memoryMarkStore.readForVideo(video.contentUri)
                                    },
                                ) { Text("Remove") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemoryMarksDialog = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        memoryMarkNote = ""
                        memoryMarkMood = null
                        customMemoryMarkMood = ""
                        showMemoryMarksDialog = false
                        showAddMemoryMarkDialog = true
                    },
                ) { Text("Add mark") }
            },
        )
    }

    if (showAddMemoryMarkDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemoryMarkDialog = false },
            title = { Text("Save this moment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("At ${formatPlaybackTime(player.currentPosition)}")
                    TextField(
                        value = memoryMarkNote,
                        onValueChange = { memoryMarkNote = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                    Text("Mood (optional)", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Calm", "Funny", "Intense").forEach { mood ->
                            FilterChip(
                                selected = memoryMarkMood == mood,
                                onClick = { memoryMarkMood = if (memoryMarkMood == mood) null else mood },
                                label = { Text(mood) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Beautiful", "Important", "Custom").forEach { mood ->
                            FilterChip(
                                selected = memoryMarkMood == mood,
                                onClick = { memoryMarkMood = if (memoryMarkMood == mood) null else mood },
                                label = { Text(mood) },
                            )
                        }
                    }
                    if (memoryMarkMood == "Custom") {
                        TextField(
                            value = customMemoryMarkMood,
                            onValueChange = { customMemoryMarkMood = it },
                            label = { Text("Custom mood") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mood = if (memoryMarkMood == "Custom") {
                            customMemoryMarkMood.trim().takeIf { it.isNotEmpty() }
                        } else {
                            memoryMarkMood
                        }
                        memoryMarkStore.add(
                            video = video,
                            positionMs = player.currentPosition,
                            note = memoryMarkNote,
                            mood = mood,
                        )
                        memoryMarks = memoryMarkStore.readForVideo(video.contentUri)
                        showAddMemoryMarkDialog = false
                        subtitleMessage = "Memory mark saved"
                        controlsVisible = true
                    },
                ) { Text("Save mark") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryMarkDialog = false }) { Text("Cancel") }
            },
        )
    }
}

private fun readAvailableSubtitle(
    context: Context,
    subtitleStore: VideoSubtitleStore,
    video: VideoItem,
): VideoSubtitleSelection? {
    val selection = subtitleStore.read(video.contentUri) ?: return null
    val hasReadAccess = context.contentResolver.persistedUriPermissions.any { permission ->
        permission.uri.toString() == selection.uri && permission.isReadPermission
    }
    if (hasReadAccess) return selection
    subtitleStore.clear(video.contentUri)
    return null
}

private fun releasePersistedSubtitleAccess(context: Context, uriString: String?) {
    val uri = uriString?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}

private fun sleepTimerLabel(remainingMs: Long): String {
    if (remainingMs <= 0L) return "Sleep timer"
    val totalSeconds = (remainingMs + 999L) / 1_000L
    return "Sleep ${totalSeconds / 60L}:${(totalSeconds % 60L).toString().padStart(2, '0')}"
}

private fun createMediaItem(
    video: VideoItem,
    subtitle: VideoSubtitleSelection?,
): MediaItem {
    val builder = MediaItem.Builder()
        .setMediaId(video.contentUri)
        .setUri(video.contentUri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(video.displayName).build())
    val mimeType = subtitle?.let { VideoSubtitleTypePolicy.mimeTypeFor(it.displayName) }
    if (subtitle != null && mimeType != null) {
        val configuration = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.uri))
            .setMimeType(mimeType)
            .setLanguage("und")
            .setLabel(subtitle.displayName)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        builder.setSubtitleConfigurations(listOf(configuration))
    }
    return builder.build()
}

private fun querySubtitleDisplayName(context: Context, uri: Uri): String {
    val queriedName = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && column >= 0) cursor.getString(column) else null
        }
    }.getOrNull()
    return queriedName?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
        ?: "subtitle"
}

private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3_600L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(totalSeconds / 60L, seconds)
    }
}

private fun VideoRepeatMode.toPlayerRepeatMode(): Int = when (this) {
    VideoRepeatMode.OFF -> Player.REPEAT_MODE_OFF
    VideoRepeatMode.ONE -> Player.REPEAT_MODE_ONE
}

private fun VideoOrientationMode.toRequestedOrientation(): Int = when (this) {
    VideoOrientationMode.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    VideoOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    VideoOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
}

private fun VideoResizeMode.toPlayerViewResizeMode(): Int = when (this) {
    VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    VideoResizeMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
}
