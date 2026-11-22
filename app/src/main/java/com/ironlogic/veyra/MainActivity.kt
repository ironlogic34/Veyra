package com.ironlogic.veyra

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ironlogic.veyra.data.video.VideoAccessPermission
import com.ironlogic.veyra.data.video.VideoAccessState
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoFolder
import com.ironlogic.veyra.data.video.VideoHistoryEntry
import com.ironlogic.veyra.data.video.VideoPlaybackHistoryStore
import com.ironlogic.veyra.data.video.VideoFolderRepository
import com.ironlogic.veyra.data.video.VideoFolderStore
import com.ironlogic.veyra.data.video.VideoLibraryMergePolicy
import com.ironlogic.veyra.data.video.VideoLibraryRepository
import com.ironlogic.veyra.data.video.VideoLibraryScanError
import com.ironlogic.veyra.data.video.VideoPlaybackProgressStore
import com.ironlogic.veyra.data.video.VideoPlaybackSettingsStore
import com.ironlogic.veyra.data.video.VideoOrientationMode
import com.ironlogic.veyra.data.video.VideoThumbnailRepository
import com.ironlogic.veyra.data.video.asVideoItem
import java.text.DateFormat
import java.util.Date
import java.io.File
import com.ironlogic.veyra.ui.theme.VeyraTheme
import com.ironlogic.veyra.ui.video.VideoPlayerScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VeyraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VeyraHome()
                }
            }
        }
    }
}

@Composable
private fun VeyraHome() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { VideoLibraryRepository(context.contentResolver) }
    val folderStore = remember { VideoFolderStore(context) }
    val folderRepository = remember { VideoFolderRepository(context) }
    val thumbnailRepository = remember {
        VideoThumbnailRepository(
            context.contentResolver,
            File(context.cacheDir, "video-thumbnails"),
        )
    }
    val progressStore = remember { VideoPlaybackProgressStore(context) }
    val historyStore = remember { VideoPlaybackHistoryStore(context) }
    val playbackSettingsStore = remember { VideoPlaybackSettingsStore(context) }
    val permissionRequests = remember {
        VideoAccessPermission.requestedPermissions(Build.VERSION.SDK_INT)
    }
    var videoAccess by remember { mutableStateOf(currentVideoAccess(context)) }
    var isLoading by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var selectedFolders by remember { mutableStateOf<List<VideoFolder>>(emptyList()) }
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }
    var continueWatching by remember { mutableStateOf(progressStore.read()) }
    var history by remember { mutableStateOf(historyStore.read()) }
    var destination by remember { mutableStateOf(VeyraDestination.LIBRARY) }
    val resumeContinueWatching: () -> Unit = {
        val progress = continueWatching
        if (progress != null) selectedVideo = progress.asVideoItem()
    }

    selectedVideo?.let { video ->
        VideoPlayerScreen(
            video = video,
            progressStore = progressStore,
            historyStore = historyStore,
            settingsStore = playbackSettingsStore,
            onBack = {
                continueWatching = progressStore.read()
                history = historyStore.read()
                selectedVideo = null
            },
        )
        return
    }

    fun refresh() {
        if (isLoading) return

        scope.launch {
            isLoading = true
            scanError = null
            try {
                var localScanError: String? = null
                val localVideos = if (videoAccess != VideoAccessState.NONE) {
                    try {
                        repository.scan()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        if (exception is SecurityException) {
                            videoAccess = currentVideoAccess(context)
                        }
                        localScanError = VideoLibraryScanError.messageFor(exception)
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                val folderResult = folderRepository.scan(folderStore.read())
                selectedFolders = folderResult.folders
                if (localScanError == null || folderResult.videos.isNotEmpty()) {
                    videos = VideoLibraryMergePolicy.merge(localVideos, folderResult.videos)
                }
                val errors = listOfNotNull(localScanError) + folderResult.errors
                scanError = errors.takeIf { it.isNotEmpty() }?.joinToString(" ")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                scanError = VideoLibraryScanError.messageFor(exception)
            } finally {
                isLoading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        videoAccess = currentVideoAccess(context)
    }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                folderStore.add(uri)
                refresh()
            } catch (_: SecurityException) {
                scanError = "Veyra couldn't keep access to that folder. Select it again and retry."
            }
        }
    }

    LaunchedEffect(videoAccess) {
        refresh()
    }

    val isTabletLandscape = LocalConfiguration.current.screenWidthDp >= 600
    val content: @Composable () -> Unit = {
        when (destination) {
            VeyraDestination.LIBRARY -> LibraryContent(
                videos = videos,
                isLoading = isLoading,
                scanError = scanError,
                hasPermission = videoAccess != VideoAccessState.NONE,
                folders = selectedFolders,
                thumbnailRepository = thumbnailRepository,
                onOpenVideo = { selectedVideo = it },
                onRequestPermission = { permissionLauncher.launch(permissionRequests) },
                onAddFolder = { folderPickerLauncher.launch(null) },
                onRemoveFolder = { folderUri ->
                    val uri = Uri.parse(folderUri)
                    folderStore.remove(uri)
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    refresh()
                },
                onRefresh = ::refresh,
            )
            VeyraDestination.HISTORY -> HistoryContent(
                entries = history,
                onOpenVideo = { entry -> selectedVideo = entry.asVideoItem() },
                onClearHistory = {
                    historyStore.clear()
                    history = emptyList()
                },
            )
            VeyraDestination.SETTINGS -> SettingsContent(playbackSettingsStore)
        }
    }

    val sectionTitle = when (destination) {
        VeyraDestination.LIBRARY -> "Your library"
        VeyraDestination.HISTORY -> "Playback history"
        VeyraDestination.SETTINGS -> "Video settings"
    }
    if (isTabletLandscape) {
        TabletLibraryShell(
            content,
            continueWatching,
            resumeContinueWatching,
            showContinueWatching = destination == VeyraDestination.LIBRARY,
            sectionTitle = sectionTitle,
            selectedDestination = destination,
            onDestinationSelected = { destination = it },
        )
    } else {
        PhoneLibraryShell(
            content,
            continueWatching,
            resumeContinueWatching,
            showContinueWatching = destination == VeyraDestination.LIBRARY,
            sectionTitle = sectionTitle,
            selectedDestination = destination,
            onDestinationSelected = { destination = it },
        )
    }
}

@Composable
private fun PhoneLibraryShell(
    content: @Composable () -> Unit,
    progress: com.ironlogic.veyra.data.video.VideoProgress?,
    onResumeContinueWatching: () -> Unit,
    showContinueWatching: Boolean,
    sectionTitle: String,
    selectedDestination: VeyraDestination,
    onDestinationSelected: (VeyraDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.background)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Header()
        if (showContinueWatching) {
            ContinueWatchingCard(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                onResume = onResumeContinueWatching,
            )
        }
        SectionTitle(sectionTitle)
        content()
        Spacer(modifier = Modifier.weight(1f))
        FloatingDock(selectedDestination, onDestinationSelected)
    }
}

@Composable
private fun TabletLibraryShell(
    content: @Composable () -> Unit,
    progress: com.ironlogic.veyra.data.video.VideoProgress?,
    onResumeContinueWatching: () -> Unit,
    showContinueWatching: Boolean,
    sectionTitle: String,
    selectedDestination: VeyraDestination,
    onDestinationSelected: (VeyraDestination) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.background)
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(
            modifier = Modifier.width(230.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("VEYRA", color = VeyraColors.accent, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text(
                "Library",
                modifier = Modifier.clickable { onDestinationSelected(VeyraDestination.LIBRARY) },
                color = if (selectedDestination == VeyraDestination.LIBRARY) Color.White else VeyraColors.muted,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "History",
                modifier = Modifier.clickable { onDestinationSelected(VeyraDestination.HISTORY) },
                color = if (selectedDestination == VeyraDestination.HISTORY) VeyraColors.accent else VeyraColors.muted,
            )
            Text(
                "Settings",
                modifier = Modifier.clickable { onDestinationSelected(VeyraDestination.SETTINGS) },
                color = if (selectedDestination == VeyraDestination.SETTINGS) VeyraColors.accent else VeyraColors.muted,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header()
            if (showContinueWatching) {
                ContinueWatchingCard(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    onResume = onResumeContinueWatching,
                )
            }
            SectionTitle(sectionTitle)
            content()
        }
    }
}

@Composable
private fun LibraryContent(
    videos: List<VideoItem>,
    isLoading: Boolean,
    scanError: String?,
    hasPermission: Boolean,
    folders: List<VideoFolder>,
    thumbnailRepository: VideoThumbnailRepository,
    onOpenVideo: (VideoItem) -> Unit,
    onRequestPermission: () -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FolderAccessSection(folders, onAddFolder, onRemoveFolder)
        when {
            !hasPermission && folders.isEmpty() -> PermissionCard(onRequestPermission)
            isLoading -> StatusCard("Looking through your local videos…")
            else -> {
                if (!hasPermission) {
                    StatusCard("Device-wide access is off. Videos from selected folders are still available.")
                }
                if (scanError != null) {
                    ScanErrorCard(scanError, onRefresh)
                }
                if (videos.isEmpty() && scanError == null) {
                    EmptyLibraryCard(onRefresh)
                } else if (videos.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(videos, key = { it.id }) { video ->
                            VideoRow(video, thumbnailRepository, onOpenVideo)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderAccessSection(
    folders: List<VideoFolder>,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.surface.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Selected folders", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    "Include videos from folders you choose.",
                    color = VeyraColors.muted,
                    fontSize = 12.sp,
                )
            }
            Button(onClick = onAddFolder) { Text("Add folder") }
        }
        folders.forEach { folder ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(folder.name, color = Color.White, maxLines = 1)
                    Text(
                        if (folder.accessible) "Available" else "Access lost",
                        color = if (folder.accessible) VeyraColors.accent else VeyraColors.muted,
                        fontSize = 11.sp,
                    )
                }
                TextButton(onClick = { onRemoveFolder(folder.uri) }) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun HistoryContent(
    entries: List<VideoHistoryEntry>,
    onOpenVideo: (VideoHistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
) {
    if (entries.isEmpty()) {
        StatusCard("Videos you play will appear here.")
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Recently played", color = VeyraColors.muted, fontSize = 13.sp)
            TextButton(onClick = onClearHistory) { Text("Clear history") }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(entries, key = { it.contentUri }) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VeyraColors.surface, RoundedCornerShape(16.dp))
                        .clickable { onOpenVideo(entry) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            "Last played " + formatHistoryDate(entry.lastPlayedEpochMs),
                            color = VeyraColors.muted,
                            fontSize = 12.sp,
                        )
                        Text(
                            "Resume from " + formatDuration(entry.positionMs),
                            color = VeyraColors.accent,
                            fontSize = 12.sp,
                        )
                    }
                    TextButton(onClick = { onOpenVideo(entry) }) { Text("Open") }
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(settingsStore: VideoPlaybackSettingsStore) {
    var resizeMode by remember { mutableStateOf(settingsStore.readResizeMode()) }
    var orientationMode by remember { mutableStateOf(settingsStore.readOrientationMode()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PreferenceCard(
            title = "Aspect ratio",
            description = "Fit shows the complete frame. Crop fills the player without stretching.",
        ) {
            SettingChoice("Fit", resizeMode == com.ironlogic.veyra.data.video.VideoResizeMode.FIT) {
                resizeMode = com.ironlogic.veyra.data.video.VideoResizeMode.FIT
                settingsStore.writeResizeMode(resizeMode)
            }
            SettingChoice("Crop", resizeMode == com.ironlogic.veyra.data.video.VideoResizeMode.CROP) {
                resizeMode = com.ironlogic.veyra.data.video.VideoResizeMode.CROP
                settingsStore.writeResizeMode(resizeMode)
            }
        }
        PreferenceCard(
            title = "Playback orientation",
            description = "Applied while a video is open. System default follows the device.",
        ) {
            VideoOrientationMode.values().forEach { mode ->
                SettingChoice(mode.label, orientationMode == mode) {
                    orientationMode = mode
                    settingsStore.writeOrientationMode(mode)
                }
            }
        }
    }
}

@Composable
private fun PreferenceCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(description, color = VeyraColors.muted, fontSize = 12.sp)
        content()
    }
}

@Composable
private fun SettingChoice(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = if (selected) Color.White else VeyraColors.muted,
        )
        RadioButton(selected = selected, onClick = onSelect)
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Your private library", color = VeyraColors.muted, style = MaterialTheme.typography.labelLarge)
        Text("Stay with your stories.", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ContinueWatchingCard(
    progress: com.ironlogic.veyra.data.video.VideoProgress?,
    modifier: Modifier = Modifier,
    onResume: () -> Unit,
) {
    Box(
        modifier = modifier
            .clickable(enabled = progress != null, onClick = onResume)
            .height(150.dp)
            .background(VeyraColors.surface, RoundedCornerShape(24.dp))
            .padding(20.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CONTINUE WATCHING", color = VeyraColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                progress?.displayName ?: "Your next memory will appear here.",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                progress?.let { "Resume from ${formatDuration(it.positionMs)}" }
                    ?: "Veyra will remember where you stopped.",
                color = VeyraColors.muted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.surface.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Let Veyra see your videos", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Your files stay on this device. Veyra only reads local video metadata until you choose something to play.", color = VeyraColors.muted)
            Button(onClick = onRequestPermission) { Text("Grant video access") }
        }
    }
}

@Composable
private fun StatusCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.surface.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) { Text(message, color = VeyraColors.muted) }
}

@Composable
private fun ScanErrorCard(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.surface.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Couldn't scan your videos", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(message, color = VeyraColors.muted)
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
private fun EmptyLibraryCard(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.surface.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your library is quiet.", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("No local videos were found yet. Add a video to the device and scan again.", color = VeyraColors.muted)
            Button(onClick = onRefresh) { Text("Scan again") }
        }
    }
}

@Composable
private fun VideoRow(
    video: VideoItem,
    thumbnailRepository: VideoThumbnailRepository,
    onOpenVideo: (VideoItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.surface, RoundedCornerShape(18.dp))
            .clickable { onOpenVideo(video) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoThumbnail(video, thumbnailRepository)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(video.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(formatDuration(video.durationMs), color = VeyraColors.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun VideoThumbnail(video: VideoItem, repository: VideoThumbnailRepository) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = video) {
        value = repository.load(video)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = video.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(76.dp)
                .height(52.dp),
        )
    } else {
        Box(
            modifier = Modifier
                .width(76.dp)
                .height(52.dp)
                .background(VeyraColors.thumbnail, RoundedCornerShape(12.dp)),
        )
    }
}

@Composable
private fun FloatingDock(
    selectedDestination: VeyraDestination,
    onDestinationSelected: (VeyraDestination) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Text(
            "Library",
            modifier = Modifier.clickable { onDestinationSelected(VeyraDestination.LIBRARY) },
            color = if (selectedDestination == VeyraDestination.LIBRARY) VeyraColors.accent else VeyraColors.muted,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "History",
            modifier = Modifier.clickable { onDestinationSelected(VeyraDestination.HISTORY) },
            color = if (selectedDestination == VeyraDestination.HISTORY) VeyraColors.accent else VeyraColors.muted,
        )
        Text(
            "Settings",
            modifier = Modifier.clickable { onDestinationSelected(VeyraDestination.SETTINGS) },
            color = if (selectedDestination == VeyraDestination.SETTINGS) VeyraColors.accent else VeyraColors.muted,
        )
    }
}

private enum class VeyraDestination {
    LIBRARY,
    HISTORY,
    SETTINGS,
}

private fun formatHistoryDate(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))

private fun currentVideoAccess(context: Context): VideoAccessState {
    val grantedPermissions = VideoAccessPermission
        .requestedPermissions(Build.VERSION.SDK_INT)
        .filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        .toSet()

    return VideoAccessPermission.resolve(Build.VERSION.SDK_INT, grantedPermissions)
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private object VeyraColors {
    val background = Color(0xFF111014)
    val surface = Color(0xFF211E25)
    val thumbnail = Color(0xFF403644)
    val muted = Color(0xFFA9A2AE)
    val accent = Color(0xFFFF9E5E)
}
