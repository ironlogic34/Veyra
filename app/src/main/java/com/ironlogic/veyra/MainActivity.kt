package com.ironlogic.veyra

import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.ironlogic.veyra.data.video.VideoLibraryRepository
import com.ironlogic.veyra.data.video.VideoPlaybackProgressStore
import com.ironlogic.veyra.data.video.VideoThumbnailRepository
import com.ironlogic.veyra.ui.theme.VeyraTheme
import com.ironlogic.veyra.ui.video.VideoPlayerScreen
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
    val thumbnailRepository = remember { VideoThumbnailRepository(context.contentResolver) }
    val progressStore = remember { VideoPlaybackProgressStore(context) }
    val permissionRequests = remember {
        VideoAccessPermission.requestedPermissions(Build.VERSION.SDK_INT)
    }
    var videoAccess by remember { mutableStateOf(currentVideoAccess(context)) }
    var isLoading by remember { mutableStateOf(false) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }
    var continueWatching by remember { mutableStateOf(progressStore.read()) }

    selectedVideo?.let { video ->
        VideoPlayerScreen(
            video = video,
            progressStore = progressStore,
            onBack = {
                continueWatching = progressStore.read()
                selectedVideo = null
            },
        )
        return
    }

    fun refresh() {
        scope.launch {
            isLoading = true
            videos = repository.scan()
            isLoading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        videoAccess = currentVideoAccess(context)
    }

    LaunchedEffect(videoAccess) {
        if (videoAccess != VideoAccessState.NONE) refresh()
    }

    val isTabletLandscape = LocalConfiguration.current.screenWidthDp >= 600
    val content: @Composable () -> Unit = {
        LibraryContent(
            videos = videos,
            isLoading = isLoading,
            hasPermission = videoAccess != VideoAccessState.NONE,
            thumbnailRepository = thumbnailRepository,
            onOpenVideo = { selectedVideo = it },
            onRequestPermission = { permissionLauncher.launch(permissionRequests) },
            onRefresh = ::refresh,
        )
    }

    if (isTabletLandscape) {
        TabletLibraryShell(content, continueWatching)
    } else {
        PhoneLibraryShell(content, continueWatching)
    }
}

@Composable
private fun PhoneLibraryShell(
    content: @Composable () -> Unit,
    progress: com.ironlogic.veyra.data.video.VideoProgress?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.background)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Header()
        ContinueWatchingCard(progress = progress, modifier = Modifier.fillMaxWidth())
        SectionTitle("Your library")
        content()
        Spacer(modifier = Modifier.weight(1f))
        FloatingDock()
    }
}

@Composable
private fun TabletLibraryShell(
    content: @Composable () -> Unit,
    progress: com.ironlogic.veyra.data.video.VideoProgress?,
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
            Text("Library", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("History", color = VeyraColors.muted)
            Text("Settings", color = VeyraColors.muted)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header()
            ContinueWatchingCard(progress = progress, modifier = Modifier.fillMaxWidth())
            SectionTitle("Your library")
            content()
        }
    }
}

@Composable
private fun LibraryContent(
    videos: List<VideoItem>,
    isLoading: Boolean,
    hasPermission: Boolean,
    thumbnailRepository: VideoThumbnailRepository,
    onOpenVideo: (VideoItem) -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
) {
    when {
        !hasPermission -> PermissionCard(onRequestPermission)
        isLoading -> StatusCard("Looking through your local videos…")
        videos.isEmpty() -> EmptyLibraryCard(onRefresh)
        else -> LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(videos, key = { it.id }) { video -> VideoRow(video, thumbnailRepository, onOpenVideo) }
        }
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
) {
    Box(
        modifier = modifier
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
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = video.contentUri) {
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
private fun FloatingDock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Text("Library", color = VeyraColors.accent, fontWeight = FontWeight.SemiBold)
        Text("History", color = VeyraColors.muted)
        Text("Settings", color = VeyraColors.muted)
    }
}

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
